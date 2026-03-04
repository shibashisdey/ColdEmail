package com.shibashis.coldmailer.v1.queue;

import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.Contact;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import com.shibashis.coldmailer.v1.repositories.CampaignRepository;
import com.shibashis.coldmailer.v1.services.EmailAccountRoutingService;
import com.shibashis.coldmailer.v1.services.EmailSenderService;
import com.shibashis.coldmailer.v1.services.PlatformPolicyService;
import com.shibashis.coldmailer.v1.services.TemplateRenderer;
import com.shibashis.coldmailer.v1.services.WorkerMetricsService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "app.worker.enabled", havingValue = "true", matchIfMissing = true)
public class EmailJobWorker {

    private static final Logger logger = LoggerFactory.getLogger(EmailJobWorker.class);

    private final EmailJobQueueService queueService;
    private final CampaignRepository campaignRepository;
    private final CampaignContactRepository campaignContactRepository;
    private final TemplateRenderer templateRenderer;
    private final EmailSenderService emailSenderService;
    private final EmailAccountRoutingService emailAccountRoutingService;
    private final PlatformPolicyService platformPolicyService;
    private final WorkerMetricsService workerMetricsService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${app.dispatch.min-delay-ms:8000}")
    private long defaultMinDelayMs;

    @Value("${app.dispatch.max-delay-ms:15000}")
    private long defaultMaxDelayMs;

    @Value("${app.dispatch.max-retries:2}")
    private int defaultMaxRetries;

    public EmailJobWorker(EmailJobQueueService queueService,
                          CampaignRepository campaignRepository,
                          CampaignContactRepository campaignContactRepository,
                          TemplateRenderer templateRenderer,
                          EmailSenderService emailSenderService,
                          EmailAccountRoutingService emailAccountRoutingService,
                          PlatformPolicyService platformPolicyService,
                          WorkerMetricsService workerMetricsService) {
        this.queueService = queueService;
        this.campaignRepository = campaignRepository;
        this.campaignContactRepository = campaignContactRepository;
        this.templateRenderer = templateRenderer;
        this.emailSenderService = emailSenderService;
        this.emailAccountRoutingService = emailAccountRoutingService;
        this.platformPolicyService = platformPolicyService;
        this.workerMetricsService = workerMetricsService;
    }

    @PostConstruct
    public void logWorkerStart() {
        logger.info("Email worker initialized and waiting for jobs.");
    }

    @Scheduled(fixedDelayString = "${app.worker.poll-delay-ms:250}")
    public void consumeQueue() {
        Optional<EmailJobPayload> maybeJob = queueService.blockingPop(Duration.ofSeconds(2));
        maybeJob.ifPresent(this::processJobSafely);
    }

    private void processJobSafely(EmailJobPayload job) {
        try {
            processJob(job);
            applyRateDelay();
        } catch (Exception ex) {
            logger.error("worker_error campaignId={} campaignContactId={} trackingId={} message={}",
                    job.getCampaignId(), job.getCampaignContactId(), job.getTrackingId(), ex.getMessage(), ex);
        }
    }

    @Transactional
    protected void processJob(EmailJobPayload job) {
        CampaignContact campaignContact = campaignContactRepository.findById(job.getCampaignContactId()).orElse(null);
        if (campaignContact == null) {
            logger.warn("job_skip reason=campaign_contact_missing campaignContactId={}", job.getCampaignContactId());
            return;
        }

        Campaign campaign = campaignContact.getCampaign();
        if (campaign == null || campaign.getStatus() == CampaignStatus.FAILED) {
            logger.warn("job_skip reason=campaign_invalid campaignContactId={} campaignId={}",
                    campaignContact.getId(), job.getCampaignId());
            return;
        }

        // Idempotency: do not send if already sent.
        if (campaignContact.getStatus() == CampaignContactStatus.SENT
                || campaignContact.getStatus() == CampaignContactStatus.OPENED
                || campaignContact.getStatus() == CampaignContactStatus.RESUME_DOWNLOADED) {
            logger.info("job_skip reason=already_sent campaignId={} campaignContactId={} trackingId={}",
                    campaign.getId(), campaignContact.getId(), campaignContact.getTrackingId());
            return;
        }

        Contact contact = campaignContact.getContact();
        Map<String, Object> vars = new HashMap<>();
        vars.put("firstName", contact.getFirstName());
        vars.put("lastName", contact.getLastName());
        vars.put("company", contact.getCompany());
        vars.put("email", contact.getEmail());
        vars.put("trackingId", campaignContact.getTrackingId().toString());
        vars.put("openTrackingUrl", baseUrl + "/api/track/open/" + campaignContact.getTrackingId());
        vars.put("resumeTrackingUrl", baseUrl + "/api/track/resume/" + campaignContact.getTrackingId());
        vars.put("resumeUrl", baseUrl + "/api/track/resume/" + campaignContact.getTrackingId());

        String renderedBody = templateRenderer.render(campaign.getTemplateBody(), vars);
        String bodyWithTrackingPixel = renderedBody
                + "<img src='" + baseUrl + "/api/track/open/" + campaignContact.getTrackingId()
                + "' width='1' height='1' style='display:none;'/>";

        var selectedAccount = emailAccountRoutingService.resolveAccount(campaign);
        try {
            emailSenderService.sendHtmlMessage(selectedAccount, contact.getEmail(), campaign.getSubject(), bodyWithTrackingPixel);
            campaignContact.setStatus(CampaignContactStatus.SENT);
            campaignContact.setSentAt(LocalDateTime.now());
            campaignContact.setFailureReason(null);
            campaignContactRepository.save(campaignContact);
            logger.info("job_sent campaignId={} campaignContactId={} trackingId={} recipient={} smtpAccountId={}",
                    campaign.getId(), campaignContact.getId(), campaignContact.getTrackingId(), contact.getEmail(), selectedAccount.getId());
            workerMetricsService.markSent();
        } catch (Exception ex) {
            int retryCount = campaignContact.getRetryCount() + 1;
            campaignContact.setRetryCount(retryCount);
            campaignContact.setFailureReason(ex.getMessage());
            campaignContact.setStatus(CampaignContactStatus.FAILED);
            campaignContactRepository.save(campaignContact);
            workerMetricsService.markFailed();

            logger.error("job_failed campaignId={} campaignContactId={} trackingId={} retry={} smtpAccountId={} message={}",
                    campaign.getId(), campaignContact.getId(), campaignContact.getTrackingId(), retryCount, selectedAccount.getId(), ex.getMessage());

            long maxRetries = platformPolicyService.getLong(PlatformPolicyService.MAX_RETRIES, defaultMaxRetries);
            if (retryCount <= maxRetries) {
                queueService.push(new EmailJobPayload(
                        campaign.getId(),
                        campaignContact.getId(),
                        campaign.getUser().getId(),
                        campaignContact.getTrackingId()
                ));
                logger.info("job_requeued campaignId={} campaignContactId={} trackingId={} retry={}",
                        campaign.getId(), campaignContact.getId(), campaignContact.getTrackingId(), retryCount);
            }
        }
        workerMetricsService.markProcessed();

        finalizeCampaignIfDone(campaign.getId());
    }

    private void finalizeCampaignIfDone(Long campaignId) {
        long pending = campaignContactRepository.countByCampaignIdAndStatus(campaignId, CampaignContactStatus.PENDING);
        if (pending > 0) {
            return;
        }
        long failed = campaignContactRepository.countByCampaignIdAndStatus(campaignId, CampaignContactStatus.FAILED);
        long sent = campaignContactRepository.countByCampaignIdAndStatusIn(
                campaignId,
                CampaignContactStatus.sentLikeStatuses()
        );
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            return;
        }

        campaign.setStatus(sent > 0 ? CampaignStatus.COMPLETED : CampaignStatus.FAILED);
        campaign.setProcessingDetails("Async dispatch complete. Sent=" + sent + ", Failed=" + failed);
        campaignRepository.save(campaign);
    }

    private void applyRateDelay() {
        long minDelayMs = platformPolicyService.getLong(PlatformPolicyService.DISPATCH_MIN_DELAY_MS, defaultMinDelayMs);
        long maxDelayMs = platformPolicyService.getLong(PlatformPolicyService.DISPATCH_MAX_DELAY_MS, defaultMaxDelayMs);
        long min = Math.min(minDelayMs, maxDelayMs);
        long max = Math.max(minDelayMs, maxDelayMs);
        long delay = ThreadLocalRandom.current().nextLong(min, max + 1);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}

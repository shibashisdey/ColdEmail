package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.CampaignCreateRequest;
import com.shibashis.coldmailer.v1.dto.CampaignFromTextRequest;
import com.shibashis.coldmailer.v1.dto.CampaignStatsDTO;
import com.shibashis.coldmailer.v1.dto.ProspectData;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.CampaignProspect;
import com.shibashis.coldmailer.v1.models.EmailTemplate;
import com.shibashis.coldmailer.v1.models.Prospect;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import com.shibashis.coldmailer.v1.repositories.CampaignProspectRepository;
import com.shibashis.coldmailer.v1.repositories.CampaignRepository;
import com.shibashis.coldmailer.v1.repositories.EmailTemplateRepository;
import com.shibashis.coldmailer.v1.repositories.ProspectRepository;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepository campaignRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final ProspectRepository prospectRepository;
    private final CampaignProspectRepository campaignProspectRepository;
    private final EmailService emailService;
    private final TemplateRenderer templateRenderer;
    private final ProspectDerivationService prospectDerivationService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public CampaignService(
            CampaignRepository campaignRepository,
            EmailTemplateRepository emailTemplateRepository,
            ProspectRepository prospectRepository,
            CampaignProspectRepository campaignProspectRepository,
            EmailService emailService,
            TemplateRenderer templateRenderer,
            ProspectDerivationService prospectDerivationService) {
        this.campaignRepository = campaignRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.prospectRepository = prospectRepository;
        this.campaignProspectRepository = campaignProspectRepository;
        this.emailService = emailService;
        this.templateRenderer = templateRenderer;
        this.prospectDerivationService = prospectDerivationService;
    }

    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional
    public Campaign createCampaign(CampaignCreateRequest request) {
        EmailTemplate template = emailTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Template ID"));

        List<Prospect> prospectsForCampaign = new ArrayList<>();
        if (request.getEmailAddresses() != null && !request.getEmailAddresses().isEmpty()) {
            
            Set<String> uniqueEmails = new HashSet<>(request.getEmailAddresses());
            int skippedCount = request.getEmailAddresses().size() - uniqueEmails.size();
            
            for (String emailAddress : uniqueEmails) {
                if (emailAddress == null || !Pattern.matches("^[\\w-_\\.+]*[\\w-_.]@([\\w]+\\.)+[\\w]+[\\w]$", emailAddress)) {
                    logger.warn("Skipping invalid email format: {}", emailAddress);
                    skippedCount++;
                    continue;
                }

                Prospect prospect = prospectRepository.findByEmail(emailAddress)
                        .orElseGet(() -> {
                            ProspectData derivedData = prospectDerivationService.deriveFromEmail(emailAddress);
                            Prospect newProspect = new Prospect();
                            newProspect.setEmail(emailAddress);
                            newProspect.setFirstName(derivedData.getFirstName());
                            newProspect.setLastName(derivedData.getLastName());
                            newProspect.setCompany(derivedData.getCompanyName());
                            return prospectRepository.save(newProspect);
                        });
                prospectsForCampaign.add(prospect);
            }
        } else {
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email addresses list cannot be empty.");
        }

        Campaign campaign = new Campaign();
        campaign.setName(request.getName());
        campaign.setTemplate(template);

        List<CampaignProspect> campaignProspects = prospectsForCampaign.stream()
                .map(prospect -> new CampaignProspect(campaign, prospect))
                .collect(Collectors.toList());
        
        campaign.setCampaignProspects(campaignProspects);

        return campaignRepository.save(campaign);
    }
    
    @Transactional
    public Campaign createCampaignFromText(CampaignFromTextRequest textRequest) {
        List<String> emails = Arrays.stream(textRequest.getEmailListAsText().split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        CampaignCreateRequest createRequest = new CampaignCreateRequest();
        createRequest.setName(textRequest.getName());
        createRequest.setTemplateId(textRequest.getTemplateId());
        createRequest.setEmailAddresses(emails);

        return createCampaign(createRequest);
    }

    @Transactional
    public Campaign startCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campaign cannot be started as it is already " + campaign.getStatus());
        }

        campaign.setStatus(CampaignStatus.RUNNING);
        Campaign savedCampaign = campaignRepository.save(campaign);

        runCampaign(savedCampaign);

        return savedCampaign;
    }

    @Async
    public void runCampaign(Campaign campaign) {
        int successfulSends = 0;
        int failedSends = 0;
        try {
            logger.info("Starting execution for campaign ID: {}", campaign.getId());
            EmailTemplate template = campaign.getTemplate();

            for (CampaignProspect campaignProspect : campaign.getCampaignProspects()) {
                Prospect prospect = campaignProspect.getProspect();
                try {
                    logger.info("Sending email to {} for campaign ID: {}", prospect.getEmail(), campaign.getId());

                    Map<String, Object> variables = new HashMap<>();
                    variables.put("firstName", prospect.getFirstName());
                    variables.put("lastName", prospect.getLastName());
                    variables.put("email", prospect.getEmail());
                    variables.put("company", prospect.getCompany());

                    String renderedBody = templateRenderer.render(template.getBody(), variables);
                    String bodyWithTracker = renderedBody + String.format(
                            "<img src='%s/api/track/open/%s' width='1' height='1' style='display:none;'/>",
                            baseUrl,
                            campaignProspect.getOpenTrackedToken()
                    );


                    emailService.sendHtmlMessage(prospect.getEmail(), template.getSubject(), bodyWithTracker);

                    campaignProspect.setSentAt(LocalDateTime.now());
                    campaignProspectRepository.save(campaignProspect);
                    successfulSends++;

                    Thread.sleep(30000);
                } catch (MessagingException e) {
                    failedSends++;
                    logger.error("Failed to send email to {} for campaign ID: {}. Error: {}", prospect.getEmail(), campaign.getId(), e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Email sending thread interrupted for campaign ID: {}", campaign.getId(), e);
                    break;
                }
            }

            campaign.setStatus(CampaignStatus.COMPLETED);
            logger.info("Campaign ID: {} completed.", campaign.getId());
        } catch (Exception e) {
            logger.error("Campaign ID: {} failed with an unexpected error.", campaign.getId(), e);
            campaign.setStatus(CampaignStatus.FAILED);
        } finally {
            String summary = String.format("Process complete. Sent: %d. Failed: %d. Total prospects in campaign: %d.", successfulSends, failedSends, campaign.getCampaignProspects().size());
            campaign.setProcessingDetails(summary);
            campaignRepository.save(campaign);
        }
    }

    public Campaign pauseCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        campaign.setStatus(CampaignStatus.PAUSED);
        return campaignRepository.save(campaign);
    }

    public CampaignStatsDTO getCampaignStats(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));

        List<CampaignProspect> campaignProspects = campaign.getCampaignProspects();
        long totalProspects = campaignProspects.size();
        long sentCount = campaignProspects.stream().filter(cp -> cp.getSentAt() != null).count();
        long openCount = campaignProspects.stream().filter(cp -> cp.getOpenedAt() != null).count();
        long clickCount = campaignProspects.stream().filter(cp -> cp.getClickedAt() != null).count();

        return new CampaignStatsDTO(totalProspects, sentCount, openCount, clickCount);
    }
}
package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import com.shibashis.coldmailer.v1.queue.EmailJobPayload;
import com.shibashis.coldmailer.v1.queue.EmailJobQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CampaignDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(CampaignDispatchService.class);

    private final CampaignContactRepository campaignContactRepository;
    private final EmailJobQueueService emailJobQueueService;

    public CampaignDispatchService(CampaignContactRepository campaignContactRepository,
                                   EmailJobQueueService emailJobQueueService) {
        this.campaignContactRepository = campaignContactRepository;
        this.emailJobQueueService = emailJobQueueService;
    }

    @Transactional
    public long enqueueCampaign(Campaign campaign) {
        validateStartState(campaign);
        List<CampaignContact> contacts = campaignContactRepository.findByCampaignIdOrderByIdAsc(campaign.getId());
        if (contacts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No contacts in campaign");
        }

        long enqueued = 0;
        for (CampaignContact cc : contacts) {
            if (cc.getStatus() != CampaignContactStatus.PENDING && cc.getStatus() != CampaignContactStatus.FAILED) {
                logger.info("enqueue_skip campaignId={} campaignContactId={} status={}",
                        campaign.getId(), cc.getId(), cc.getStatus());
                continue;
            }
            emailJobQueueService.push(new EmailJobPayload(
                    campaign.getId(),
                    cc.getId(),
                    campaign.getUser().getId(),
                    cc.getTrackingId()
            ));
            enqueued++;
            logger.info("enqueue_job campaignId={} campaignContactId={} trackingId={}",
                    campaign.getId(), cc.getId(), cc.getTrackingId());
        }
        logger.info("enqueue_campaign_complete campaignId={} enqueuedJobs={}", campaign.getId(), enqueued);
        return enqueued;
    }

    private void validateStartState(Campaign campaign) {
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Campaign is already " + campaign.getStatus());
        }
        if (campaign.getResumeStoredFileName() == null || campaign.getResumeStoredFileName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Upload resume before starting campaign");
        }
    }
}

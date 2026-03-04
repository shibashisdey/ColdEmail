package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.dto.CampaignStatsDTO;
import com.shibashis.coldmailer.v1.dto.campaign.CampaignProgressDTO;
import com.shibashis.coldmailer.v1.models.Campaign;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import com.shibashis.coldmailer.v1.queue.EmailJobQueueService;
import com.shibashis.coldmailer.v1.repositories.CampaignContactRepository;
import org.springframework.stereotype.Service;

@Service
public class CampaignProgressService {

    private final CampaignContactRepository campaignContactRepository;
    private final EmailJobQueueService emailJobQueueService;

    public CampaignProgressService(CampaignContactRepository campaignContactRepository,
                                   EmailJobQueueService emailJobQueueService) {
        this.campaignContactRepository = campaignContactRepository;
        this.emailJobQueueService = emailJobQueueService;
    }

    public CampaignStatsDTO getStats(Campaign campaign) {
        long total = campaignContactRepository.countByCampaignId(campaign.getId());
        long pending = campaignContactRepository.countByCampaignIdAndStatus(campaign.getId(), CampaignContactStatus.PENDING);
        long sent = campaignContactRepository.countByCampaignIdAndStatusIn(campaign.getId(), CampaignContactStatus.sentLikeStatuses());
        long failed = campaignContactRepository.countByCampaignIdAndStatus(campaign.getId(), CampaignContactStatus.FAILED);
        long opened = campaignContactRepository.countByCampaignIdAndOpenedAtIsNotNull(campaign.getId());
        long resume = campaignContactRepository.countByCampaignIdAndResumeDownloadedAtIsNotNull(campaign.getId());
        return new CampaignStatsDTO(total, pending, sent, failed, opened, resume);
    }

    public CampaignProgressDTO getProgress(Campaign campaign) {
        CampaignStatsDTO stats = getStats(campaign);
        return new CampaignProgressDTO(
                campaign.getId(),
                campaign.getStatus(),
                stats.getTotalContacts(),
                stats.getPendingCount(),
                stats.getSentCount(),
                stats.getFailedCount(),
                stats.getOpenedCount(),
                stats.getResumeDownloadedCount(),
                emailJobQueueService.size()
        );
    }
}

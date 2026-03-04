package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.CampaignContact;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignContactRepository extends JpaRepository<CampaignContact, Long> {
    List<CampaignContact> findByCampaignIdOrderByIdAsc(Long campaignId);
    Optional<CampaignContact> findByTrackingId(UUID trackingId);
    long countByCampaignId(Long campaignId);
    long countByCampaignIdAndStatus(Long campaignId, CampaignContactStatus status);
    long countByCampaignIdAndStatusIn(Long campaignId, List<CampaignContactStatus> statuses);
    long countByCampaignIdAndOpenedAtIsNotNull(Long campaignId);
    long countByCampaignIdAndResumeDownloadedAtIsNotNull(Long campaignId);
    List<CampaignContact> findByStatusOrderByIdDesc(CampaignContactStatus status);
    long countByStatus(CampaignContactStatus status);
    long countByStatusIn(List<CampaignContactStatus> statuses);
    long countByOpenedAtIsNotNull();
    long countByResumeDownloadedAtIsNotNull();
}

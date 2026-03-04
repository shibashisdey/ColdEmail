package com.shibashis.coldmailer.v1.dto.campaign;

import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CampaignProgressDTO {
    private Long campaignId;
    private CampaignStatus campaignStatus;
    private long totalContacts;
    private long pendingCount;
    private long sentCount;
    private long failedCount;
    private long openedCount;
    private long resumeDownloadedCount;
    private long queueDepth;
}

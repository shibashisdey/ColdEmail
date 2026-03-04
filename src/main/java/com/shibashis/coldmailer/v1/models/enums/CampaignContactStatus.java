package com.shibashis.coldmailer.v1.models.enums;

import java.util.List;

public enum CampaignContactStatus {
    PENDING,
    SENT,
    FAILED,
    OPENED,
    RESUME_DOWNLOADED;

    public static List<CampaignContactStatus> sentLikeStatuses() {
        return List.of(SENT, OPENED, RESUME_DOWNLOADED);
    }
}

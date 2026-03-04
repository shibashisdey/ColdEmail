package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminPlatformStatsView {
    private long totalUsers;
    private long activeUsers;
    private long totalCampaigns;
    private long totalContacts;
    private long totalSent;
    private long totalOpened;
    private long totalResumeDownloaded;
    private long totalFailed;
    private double openRatePct;
    private double resumeClickRatePct;
    private double failureRatePct;
    private long queueDepth;
}

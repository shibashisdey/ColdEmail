package com.shibashis.coldmailer.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CampaignStatsDTO {
    private long totalProspects;
    private long sentCount;
    private long openCount;
    private long clickCount; // Will be 0 for now
}

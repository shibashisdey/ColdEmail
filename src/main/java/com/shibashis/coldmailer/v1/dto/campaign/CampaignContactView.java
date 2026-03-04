package com.shibashis.coldmailer.v1.dto.campaign;

import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CampaignContactView {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String company;
    private CampaignContactStatus status;
    private UUID trackingId;
    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime resumeDownloadedAt;
    private String failureReason;
    private int retryCount;
}

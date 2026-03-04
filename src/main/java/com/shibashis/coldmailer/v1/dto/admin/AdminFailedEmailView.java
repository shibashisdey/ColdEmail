package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminFailedEmailView {
    private Long campaignContactId;
    private Long campaignId;
    private String campaignName;
    private UUID tenantId;
    private String recipientEmail;
    private String failureReason;
    private int retryCount;
    private LocalDateTime sentAt;
}

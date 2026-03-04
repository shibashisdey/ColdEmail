package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminEmailAccountView {
    private Long emailAccountId;
    private Long userId;
    private UUID tenantId;
    private String label;
    private String smtpHost;
    private Integer smtpPort;
    private String smtpUsername;
    private String fromEmail;
    private boolean active;
    private String suspensionReason;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
}

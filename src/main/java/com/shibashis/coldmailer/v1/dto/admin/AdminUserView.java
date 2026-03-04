package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminUserView {
    private Long userId;
    private UUID tenantId;
    private String name;
    private String email;
    private boolean active;
    private String suspensionReason;
    private LocalDateTime suspendedAt;
    private LocalDateTime createdAt;
}

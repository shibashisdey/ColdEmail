package com.shibashis.coldmailer.v1.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AdminHrContactView {
    private Long contactId;
    private UUID tenantId;
    private String email;
    private String firstName;
    private String lastName;
    private String company;
    private LocalDateTime createdAt;
}

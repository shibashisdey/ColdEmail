package com.shibashis.coldmailer.v1.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "email_accounts")
@Getter
@Setter
public class EmailAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @NotBlank
    @Column(nullable = false)
    private String label;

    @NotBlank
    @Column(nullable = false)
    private String smtpHost;

    @Column(nullable = false)
    private Integer smtpPort;

    @NotBlank
    @Column(nullable = false)
    private String smtpUsername;

    @NotBlank
    @Column(nullable = false)
    @JsonIgnore
    private String smtpPassword;

    @NotBlank
    @Column(nullable = false)
    private String fromEmail;

    @Column(nullable = false)
    private boolean useTls = true;

    @Column(nullable = false)
    private boolean isDefault = false;

    @Column(nullable = false)
    private boolean active = true;

    private String suspensionReason;

    private LocalDateTime suspendedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (tenantId == null && user != null) {
            tenantId = user.getTenantId();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

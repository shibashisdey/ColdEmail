package com.shibashis.coldmailer.v1.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_account_id", nullable = false)
    @JsonIgnore
    private EmailAccount emailAccount;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String subject;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String templateBody;

    @Column(nullable = false)
    private boolean loadBalancedDispatch = false;

    private String resumeOriginalFileName;

    private String resumeStoredFileName;

    private String resumeContentType;

    private LocalDateTime resumeUploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String processingDetails;

    @PrePersist
    public void onCreate() {
        if (tenantId == null && user != null) {
            tenantId = user.getTenantId();
        }
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

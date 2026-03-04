package com.shibashis.coldmailer.v1.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.shibashis.coldmailer.v1.models.enums.CampaignContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "campaign_contacts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_campaign_contact_campaign_contact", columnNames = {"campaign_id", "contact_id"})
}, indexes = {
        @Index(name = "idx_campaign_contacts_tracking_id", columnList = "tracking_id"),
        @Index(name = "idx_campaign_contacts_campaign_id", columnList = "campaign_id"),
        @Index(name = "idx_campaign_contacts_status", columnList = "status")
})
@Getter
@Setter
public class CampaignContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    @JsonIgnore
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id", nullable = false)
    private Contact contact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignContactStatus status;

    @Column(name = "tracking_id", nullable = false, unique = true)
    private UUID trackingId;

    private LocalDateTime sentAt;

    private LocalDateTime openedAt;

    private LocalDateTime resumeDownloadedAt;

    private String failureReason;

    @Column(nullable = false)
    private int retryCount = 0;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = CampaignContactStatus.PENDING;
        }
        if (trackingId == null) {
            trackingId = UUID.randomUUID();
        }
    }
}

package com.shibashis.coldmailer.v1.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
public class CampaignProspect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    @JsonBackReference
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prospect_id", nullable = false)
    private Prospect prospect;

    @Column(unique = true, nullable = false)
    private String openTrackedToken;

    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    
    public CampaignProspect(Campaign campaign, Prospect prospect) {
        this.campaign = campaign;
        this.prospect = prospect;
    }

    @PrePersist
    protected void onCreate() {
        if (openTrackedToken == null) {
            openTrackedToken = UUID.randomUUID().toString();
        }
    }
}
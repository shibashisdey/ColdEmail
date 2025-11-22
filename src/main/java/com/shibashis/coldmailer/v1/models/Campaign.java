package com.shibashis.coldmailer.v1.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.shibashis.coldmailer.v1.models.enums.CampaignStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Campaign name is mandatory")
    private String name;

    @Enumerated(EnumType.STRING)
    private CampaignStatus status;

    @ManyToOne
    @JoinColumn(name = "template_id")
    private EmailTemplate template;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CampaignProspect> campaignProspects = new ArrayList<>();

    private LocalDateTime createdAt;
    
    private LocalDateTime scheduledTime;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = CampaignStatus.DRAFT;
        }
    }

    public void setCampaignProspects(List<CampaignProspect> campaignProspects) {
        this.campaignProspects.clear();
        if (campaignProspects != null) {
            this.campaignProspects.addAll(campaignProspects);
        }
    }
}

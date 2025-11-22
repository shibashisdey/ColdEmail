package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.CampaignProspect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignProspectRepository extends JpaRepository<CampaignProspect, Long> {
    Optional<CampaignProspect> findByOpenTrackedToken(String openTrackedToken);
}

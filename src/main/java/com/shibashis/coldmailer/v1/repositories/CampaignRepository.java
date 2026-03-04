package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {
    List<Campaign> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Optional<Campaign> findByIdAndTenantId(Long id, UUID tenantId);
    long countByStatus(com.shibashis.coldmailer.v1.models.enums.CampaignStatus status);
}

package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.EmailAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {
    List<EmailAccount> findByTenantId(UUID tenantId);
    List<EmailAccount> findByTenantIdAndActiveTrue(UUID tenantId);
    Optional<EmailAccount> findByIdAndTenantId(Long id, UUID tenantId);
    Optional<EmailAccount> findByTenantIdAndIsDefaultTrue(UUID tenantId);
    List<EmailAccount> findAllByOrderByCreatedAtDesc();
}

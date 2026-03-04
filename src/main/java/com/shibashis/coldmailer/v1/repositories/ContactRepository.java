package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    Optional<Contact> findByTenantIdAndEmail(UUID tenantId, String email);
    List<Contact> findByEmailContainingIgnoreCaseOrCompanyContainingIgnoreCaseOrderByCreatedAtDesc(String email, String company);
    List<Contact> findAllByOrderByCreatedAtDesc();
}

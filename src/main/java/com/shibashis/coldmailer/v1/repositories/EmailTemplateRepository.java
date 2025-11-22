package com.shibashis.coldmailer.v1.repositories;

import com.shibashis.coldmailer.v1.models.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
}

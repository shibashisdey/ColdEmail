package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.models.EmailTemplate;
import com.shibashis.coldmailer.v1.repositories.EmailTemplateRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
public class EmailTemplateController {

    private final EmailTemplateRepository emailTemplateRepository;

    @Autowired
    public EmailTemplateController(EmailTemplateRepository emailTemplateRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
    }

    @PostMapping
    public ResponseEntity<EmailTemplate> createTemplate(@Valid @RequestBody EmailTemplate emailTemplate) {
        EmailTemplate savedTemplate = emailTemplateRepository.save(emailTemplate);
        return new ResponseEntity<>(savedTemplate, HttpStatus.CREATED);
    }

    @GetMapping
    public List<EmailTemplate> getAllTemplates() {
        return emailTemplateRepository.findAll();
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailTemplate> updateTemplate(@PathVariable Long id, @Valid @RequestBody EmailTemplate templateDetails) {
        return emailTemplateRepository.findById(id)
                .map(existingTemplate -> {
                    existingTemplate.setName(templateDetails.getName());
                    existingTemplate.setSubject(templateDetails.getSubject());
                    existingTemplate.setBody(templateDetails.getBody());
                    EmailTemplate updatedTemplate = emailTemplateRepository.save(existingTemplate);
                    return new ResponseEntity<>(updatedTemplate, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}

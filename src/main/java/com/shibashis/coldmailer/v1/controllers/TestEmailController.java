package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.TestEmailRequest;
import com.shibashis.coldmailer.v1.services.EmailService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestEmailController {

    private final EmailService emailService;

    @Autowired
    public TestEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/test-email")
    public ResponseEntity<String> sendTestEmail(@Valid @RequestBody TestEmailRequest request) {
        try {
            emailService.sendSimpleMessage(request.getTo(), "Test Email", "This is a test email from the Coldmailer service.");
            return ResponseEntity.ok("Test email sent successfully to " + request.getTo());
        } catch (Exception e) {
            // It's good practice to log the exception here
            return ResponseEntity.internalServerError().body("Failed to send test email: " + e.getMessage());
        }
    }
}

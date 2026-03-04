package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.emailaccount.EmailAccountRequest;
import com.shibashis.coldmailer.v1.models.EmailAccount;
import com.shibashis.coldmailer.v1.services.EmailAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email-accounts")
public class EmailAccountController {

    private final EmailAccountService emailAccountService;

    public EmailAccountController(EmailAccountService emailAccountService) {
        this.emailAccountService = emailAccountService;
    }

    @GetMapping
    public List<EmailAccount> list() {
        return emailAccountService.listMyAccounts();
    }

    @PostMapping
    public ResponseEntity<EmailAccount> create(@Valid @RequestBody EmailAccountRequest request) {
        return new ResponseEntity<>(emailAccountService.create(request), HttpStatus.CREATED);
    }
}

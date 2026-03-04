package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.auth.AuthResponse;
import com.shibashis.coldmailer.v1.dto.auth.LoginRequest;
import com.shibashis.coldmailer.v1.dto.auth.RegisterRequest;
import com.shibashis.coldmailer.v1.services.AdminAuthService;
import com.shibashis.coldmailer.v1.services.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AdminAuthService adminAuthService;

    public AuthController(AuthService authService, AdminAuthService adminAuthService) {
        this.authService = authService;
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }
}

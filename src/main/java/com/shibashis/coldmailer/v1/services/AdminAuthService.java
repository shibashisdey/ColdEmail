package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.auth.JwtService;
import com.shibashis.coldmailer.v1.dto.auth.AuthResponse;
import com.shibashis.coldmailer.v1.dto.auth.LoginRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {

    private static final String ADMIN_EMAIL = "admin@coldemail.local";
    private static final String ADMIN_PASSWORD = "Admin@123";

    private final JwtService jwtService;

    public AdminAuthService(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase();
        String password = request.getPassword() == null ? "" : request.getPassword();

        if (!ADMIN_EMAIL.equalsIgnoreCase(email) || !ADMIN_PASSWORD.equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid admin credentials");
        }

        String token = jwtService.generateAdminToken(ADMIN_EMAIL);
        return new AuthResponse(token, "Bearer", 0L, ADMIN_EMAIL, "ADMIN");
    }
}

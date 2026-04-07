package com.shibashis.coldmailer.v1.services;

import com.shibashis.coldmailer.v1.auth.JwtService;
import com.shibashis.coldmailer.v1.auth.UserPrincipal;
import com.shibashis.coldmailer.v1.dto.auth.AuthResponse;
import com.shibashis.coldmailer.v1.dto.auth.LoginRequest;
import com.shibashis.coldmailer.v1.dto.auth.RegisterRequest;
import com.shibashis.coldmailer.v1.models.User;
import com.shibashis.coldmailer.v1.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SystemEmailAccountService systemEmailAccountService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       SystemEmailAccountService systemEmailAccountService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.systemEmailAccountService = systemEmailAccountService;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        systemEmailAccountService.ensureSystemAccount(saved);
        UserPrincipal principal = new UserPrincipal(saved);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, "Bearer", saved.getId(), saved.getEmail(), saved.getTenantId().toString());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is suspended");
        }

        systemEmailAccountService.ensureSystemAccount(user);
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtService.generateToken(principal);

        return new AuthResponse(token, "Bearer", user.getId(), user.getEmail(), user.getTenantId().toString());
    }
}

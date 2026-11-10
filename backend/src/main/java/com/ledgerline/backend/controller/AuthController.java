package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.service.AuthService;
import com.ledgerline.backend.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final RegistrationService registration;

    public AuthController(AuthService auth, RegistrationService registration) {
        this.auth = auth; this.registration = registration;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) { return registration.register(req); }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("userId", jwt.getSubject(),
                      "username", jwt.getClaimAsString("username"),
                      "role", jwt.getClaimAsString("role"));
    }
}
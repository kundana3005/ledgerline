package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.service.AuthService;
import com.ledgerline.backend.service.PasswordService;
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
    private final PasswordService passwords;

    public AuthController(AuthService auth, RegistrationService registration, PasswordService passwords) {
        this.auth = auth; this.registration = registration; this.passwords = passwords;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) { return auth.login(req); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest req) { return registration.register(req); }

    @PostMapping("/forgot-password")
    public Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        passwords.forgot(req.email());
        return Map.of("message", "If that email is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest req) {
        passwords.reset(req);
        return Map.of("message", "Password updated. You can now sign in.");
    }

    @PostMapping("/change-password")
    public Map<String, String> change(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest req) {
        passwords.change(Long.valueOf(jwt.getSubject()), req);
        return Map.of("message", "Password changed.");
    }

    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("userId", jwt.getSubject(),
                      "username", jwt.getClaimAsString("username"),
                      "role", jwt.getClaimAsString("role"));
    }
}
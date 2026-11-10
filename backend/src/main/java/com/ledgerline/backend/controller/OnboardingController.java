package com.ledgerline.backend.controller;

import com.ledgerline.backend.dto.*;
import com.ledgerline.backend.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {
    private final OnboardingService onboarding;

    public OnboardingController(OnboardingService onboarding) { this.onboarding = onboarding; }

    @GetMapping("/status")
    public OnboardingStatus status(@AuthenticationPrincipal Jwt jwt) {
        return onboarding.status(Long.valueOf(jwt.getSubject()));
    }

    @PostMapping("/account-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountRequestView submit(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenAccountRequest req) {
        return onboarding.submit(Long.valueOf(jwt.getSubject()), req.type());
    }
}
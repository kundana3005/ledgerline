package com.ledgerline.backend.controller;

import com.ledgerline.backend.service.AnalyticsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
public class AnalyticsController {
    private final AnalyticsService analytics;
    public AnalyticsController(AnalyticsService analytics) { this.analytics = analytics; }

    @GetMapping("/api/analytics/me")      // account holders only (see SecurityConfig)
    public Map<String, Object> mine(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "6") int months) {
        return analytics.holder(Long.valueOf(jwt.getSubject()), months);
    }

    @GetMapping("/api/admin/analytics")   // admins only (see SecurityConfig)
    public Map<String, Object> admin(@RequestParam(defaultValue = "30") int days) { return analytics.admin(days); }
}
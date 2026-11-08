package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.User;
import java.time.Instant;

public record UserSummary(Long id, String username, String email, String role, boolean enabled, Instant createdAt) {
    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getEmail(), u.getRole().name(),
                u.isEnabled(), u.getCreatedAt());
    }
}
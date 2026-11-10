package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.User;
import java.time.Instant;

public record UserSummary(Long id, String username, String fullName, String email, String phone, String role,
                          String status, boolean enabled, String rejectionReason, Instant createdAt) {
    public static UserSummary from(User u) {
        return new UserSummary(u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getPhone(),
                u.getRole().name(), u.getStatus().name(), u.isEnabled(), u.getRejectionReason(), u.getCreatedAt());
    }
}
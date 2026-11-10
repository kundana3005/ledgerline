package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.AccountRequest;
import java.time.Instant;

public record AccountRequestAdminView(Long id, Long userId, String fullName, String email, String type,
                                      String status, String rejectionReason, Instant createdAt, Instant reviewedAt) {
    public static AccountRequestAdminView from(AccountRequest r) {
        return new AccountRequestAdminView(r.getId(), r.getUser().getId(), r.getUser().getFullName(),
                r.getUser().getEmail(), r.getAccountType().name(), r.getStatus().name(),
                r.getRejectionReason(), r.getCreatedAt(), r.getReviewedAt());
    }
}
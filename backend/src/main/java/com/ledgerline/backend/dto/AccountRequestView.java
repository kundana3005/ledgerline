package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.AccountRequest;
import java.time.Instant;

public record AccountRequestView(Long id, String type, String status, String rejectionReason,
                                 Instant createdAt, Instant reviewedAt) {
    public static AccountRequestView from(AccountRequest r) {
        return new AccountRequestView(r.getId(), r.getAccountType().name(), r.getStatus().name(),
                r.getRejectionReason(), r.getCreatedAt(), r.getReviewedAt());
    }
}
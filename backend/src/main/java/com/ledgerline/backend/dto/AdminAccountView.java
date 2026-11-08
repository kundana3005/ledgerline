package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.Account;
import com.ledgerline.backend.entity.User;
import java.math.BigDecimal;
import java.time.Instant;

public record AdminAccountView(Long id, String accountNumber, String type, BigDecimal balance, String status,
                               Long ownerId, String ownerUsername, Instant createdAt) {
    public static AdminAccountView from(Account a) {
        User o = a.getOwner();
        return new AdminAccountView(a.getId(), a.getAccountNumber(), a.getType().name(), a.getBalance(),
                a.getStatus().name(), o == null ? null : o.getId(), o == null ? null : o.getUsername(),
                a.getCreatedAt());
    }
}
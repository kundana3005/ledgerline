package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.Account;
import java.math.BigDecimal;

public record AccountResponse(Long id, String accountNumber, String type, BigDecimal balance, String status) {
    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getType().name(),
                a.getBalance(), a.getStatus().name());
    }
}
package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.AccountType;
import jakarta.validation.constraints.NotNull;

public record OpenAccountRequest(@NotNull AccountType type) {}
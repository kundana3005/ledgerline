package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record SetAccountStatusRequest(@NotNull AccountStatus status) {}
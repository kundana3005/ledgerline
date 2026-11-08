package com.ledgerline.backend.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferRequest(
        @NotBlank String sourceAccount,
        @NotBlank String destinationAccount,
        @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
        @Digits(integer = 13, fraction = 2) BigDecimal amount) {}
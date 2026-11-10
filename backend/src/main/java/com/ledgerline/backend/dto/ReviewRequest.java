package com.ledgerline.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(@NotNull Boolean approved, @Size(max = 255) String reason) {}
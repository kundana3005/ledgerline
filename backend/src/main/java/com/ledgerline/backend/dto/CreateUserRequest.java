package com.ledgerline.backend.dto;

import com.ledgerline.backend.entity.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,50}$",
                message = "must be 3-50 letters, digits, dot, dash or underscore") String username,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
        @NotNull Role role) {}
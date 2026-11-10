package com.ledgerline.backend.dto;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotBlank @Email @Size(max = 120) String email,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$",
                message = "must be 10-15 digits with an optional leading +") String phone,
        @NotBlank @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$",
                message = "must be 8-72 characters with upper and lower case letters, a digit and a symbol") String password,
        @NotBlank String confirmPassword) {}
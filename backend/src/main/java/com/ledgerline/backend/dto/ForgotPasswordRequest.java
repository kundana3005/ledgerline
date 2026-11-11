// ForgotPasswordRequest.java
package com.ledgerline.backend.dto;
import jakarta.validation.constraints.*;
public record ForgotPasswordRequest(@NotBlank @Size(max = 120) String email) {}
// ResetPasswordRequest.java
package com.ledgerline.backend.dto;
import jakarta.validation.constraints.*;
public record ResetPasswordRequest(@NotBlank @Size(max = 100) String token, @NotBlank String password,
                                   @NotBlank String confirmPassword) {}
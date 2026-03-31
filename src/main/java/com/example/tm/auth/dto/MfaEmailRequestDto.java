package com.example.tm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Transfers mfa email request dto data between layers.
 */
@Data
public class MfaEmailRequestDto {

    @Email(message = "email must be valid")
    @NotBlank(message = "email is required")
    private String email;
}

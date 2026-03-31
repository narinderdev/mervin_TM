package com.example.tm.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Transfers mfa login dto data between layers.
 */
@Data
public class MfaLoginDto {

    @NotBlank(message = "mfaToken is required")
    @JsonProperty("mfa_token")
    private String mfaToken;

    @NotBlank(message = "code is required")
    @Pattern(regexp = "\\d{6}", message = "code must be 6 digits")
    private String code;
}

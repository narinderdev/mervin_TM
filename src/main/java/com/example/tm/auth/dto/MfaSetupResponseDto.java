package com.example.tm.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Transfers mfa setup response dto data between layers.
 */
@Getter
@AllArgsConstructor
public class MfaSetupResponseDto {

    private String secret;
    private String qrCodeImage;
}

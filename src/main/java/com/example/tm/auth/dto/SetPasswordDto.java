package com.example.tm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Transfers set password dto data between layers.
 */
@Getter
@Setter
public class SetPasswordDto {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}

package com.example.tm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "email cannot be null")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "password cannot be empty")
    private String password;

    private String deviceToken;
    private String devicePlatform;
}

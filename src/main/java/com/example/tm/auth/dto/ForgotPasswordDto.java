package com.example.tm.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Transfers forgot password dto data between layers.
 */
@Data
public class ForgotPasswordDto {

    @Email(message = "Please provide a valid email address")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "New password is required")
    @Size(min = 12, message = "Password must be at least 12 characters long")
    @Pattern(regexp = ".*[A-Z].*", message = "Password must include at least one uppercase letter")
    @Pattern(regexp = ".*[a-z].*", message = "Password must include at least one lowercase letter")
    @Pattern(regexp = ".*[^A-Za-z0-9\\s].*", message = "Password must include at least one special character")
    private String newPassword;
}

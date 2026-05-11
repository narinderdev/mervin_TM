package com.example.tm.auth.controller;

import com.example.tm.auth.dto.ChangePasswordDto;
import com.example.tm.auth.dto.ForgotPasswordDto;
import com.example.tm.auth.service.TmAuthService;
import com.example.tm.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes API endpoints for password controller.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class PasswordController {

    private final TmAuthService tmAuthService;

    /** Handles change password. */
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        tmAuthService.changePassword(dto);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Password updated successfully", null)
        );
    }

    /** Handles forgot password. */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordDto dto) {
        tmAuthService.forgotPassword(dto);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Password reset successfully", null)
        );
    }
}

package com.example.tm.auth.controller;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.dto.MfaEmailRequestDto;
import com.example.tm.auth.dto.MfaEmailVerifyDto;
import com.example.tm.auth.dto.MfaLoginDto;
import com.example.tm.auth.dto.SignupRequestDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.service.MfaService;
import com.example.tm.auth.service.TmAuthService;
import com.example.tm.shared.web.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes API endpoints for auth controller.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TmAuthService tmAuthService;
    private final MfaService mfaService;

    /** Handles login. */
    @PostMapping
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto request) {
        LoginResponseDto data = tmAuthService.login(request);
        String message = Boolean.TRUE.equals(data.getMfaRequired()) ? "MFA required" : "Login Successfully";
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), message, data)
        );
    }

    /** Handles mfa login. */
    @PostMapping("/login/mfa")
    public ResponseEntity<ApiResponse<LoginResponseDto>> loginWithMfa(@Valid @RequestBody MfaLoginDto request) {
        LoginResponseDto data = tmAuthService.loginWithMfa(request);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Login Successfully", data)
        );
    }

    /** Handles send mfa email otp. */
    @PostMapping("/mfa/email/send")
    public ResponseEntity<ApiResponse<Void>> sendMfaEmailOtp(@Valid @RequestBody MfaEmailRequestDto request) {
        mfaService.sendEmailOtp(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Email OTP sent successfully", null)
        );
    }

    /** Handles verify mfa email otp. */
    @PostMapping("/mfa/email/verify")
    public ResponseEntity<ApiResponse<Void>> verifyMfaEmailOtp(@Valid @RequestBody MfaEmailVerifyDto request) {
        mfaService.verifyEmailOtp(request.getEmail(), request.getCode());
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Email OTP verified successfully", null)
        );
    }

    /** Handles signup. */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSummaryDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        UserSummaryDto data = tmAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.successResponse(HttpStatus.CREATED.value(), "User created successfully", data)
        );
    }

    /** Returns logged in users. */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserSummaryDto>>> getLoggedInUsers() {
        List<UserSummaryDto> data = tmAuthService.getLoggedInUsers();
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Active users fetched successfully", data)
        );
    }
}

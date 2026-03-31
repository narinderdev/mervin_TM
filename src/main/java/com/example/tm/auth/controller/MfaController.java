package com.example.tm.auth.controller;

import com.example.tm.auth.dto.MfaCodeDto;
import com.example.tm.auth.dto.MfaSetupResponseDto;
import com.example.tm.auth.security.TmJwtService;
import com.example.tm.auth.service.MfaService;
import com.example.tm.shared.web.ApiResponse;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes API endpoints for mfa controller.
 */
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;
    private final TmJwtService tmJwtService;

    /** Handles setup. */
    @GetMapping("/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponseDto>> setup(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String email = currentUserEmail(authorizationHeader);
        MfaSetupResponseDto response = mfaService.setup(email);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "MFA setup initialized", response)
        );
    }

    /** Handles verify setup. */
    @PostMapping("/verify-setup")
    public ResponseEntity<ApiResponse<Void>> verifySetup(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody MfaCodeDto request) {
        String email = currentUserEmail(authorizationHeader);
        mfaService.verifySetup(email, request.getCode());
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "MFA enabled successfully", null)
        );
    }

    /** Handles disable. */
    @PostMapping("/disable")
    public ResponseEntity<ApiResponse<Void>> disable(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody MfaCodeDto request) {
        String email = currentUserEmail(authorizationHeader);
        mfaService.disable(email, request.getCode());
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "MFA disabled successfully", null)
        );
    }

    /** Handles current user email. */
    private String currentUserEmail(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }
        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            Claims claims = tmJwtService.parseClaims(token);
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
            String email = claims.getSubject();
            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
            }
            return email.trim().toLowerCase();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }
}

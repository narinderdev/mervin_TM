package com.example.tm.eam.controller;

import com.example.tm.auth.security.TmJwtService;
import com.example.tm.eam.dto.SecurityRoleReportItem;
import com.example.tm.eam.service.EamSecurityReportService;
import com.example.tm.shared.web.ApiResponse;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes API endpoints for security report controller.
 */
@RestController
@RequestMapping("/api/reports/security")
@RequiredArgsConstructor
public class SecurityReportController {

    private final EamSecurityReportService securityReportService;
    private final TmJwtService tmJwtService;

    /** Returns security report grouped by roles. */
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<SecurityRoleReportItem>>> byRole(
            @RequestParam("companyId") Long companyId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        requireManagerRole(extractRoleFromAuthorizationHeader(authorizationHeader));
        List<SecurityRoleReportItem> data = securityReportService.getRolePermissionReport(companyId);
        return ResponseEntity.ok(ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Security report by role fetched successfully",
                data));
    }

    /** Extracts role claim from bearer token. */
    private String extractRoleFromAuthorizationHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank() || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(7).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        try {
            return tmJwtService.extractPrimaryRole(token);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
    }

    /** Validates manager/admin role. */
    private void requireManagerRole(String actorRole) {
        String normalized = actorRole == null ? "" : actorRole.trim().toUpperCase(Locale.ROOT);
        boolean allowed = normalized.equals("MANAGER")
                || normalized.contains("MANAGER")
                || normalized.equals("ADMIN")
                || normalized.contains("ADMIN");
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only manager can perform this action");
        }
    }
}

package com.example.tm.eam.controller;

import com.example.tm.auth.security.TmJwtService;
import com.example.tm.eam.dto.RoleCreateRequest;
import com.example.tm.eam.dto.RoleListResponse;
import com.example.tm.eam.dto.RoleResponse;
import com.example.tm.eam.service.EamRolePermissionService;
import com.example.tm.shared.constants.HeaderConstants;
import com.example.tm.shared.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes API endpoints for role controller.
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Slf4j
public class RoleController {

    private final EamRolePermissionService eamRolePermissionService;
    private final TmJwtService tmJwtService;

    /** Returns roles. */
    @GetMapping
    public ResponseEntity<ApiResponse<RoleListResponse>> getRoles(
            @RequestParam("companyId") Long companyId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {
        requireManagerRole(extractRoleFromAuthorizationHeader(authorizationHeader));
        log.info("EAM GET /roles companyId={} page={} size={} cid={}", companyId, page, size, correlationId(request));
        RoleListResponse data = eamRolePermissionService.getRoles(companyId, page, size);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Roles fetched successfully", data));
    }

    /** Returns role by id. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(
            @PathVariable("id") Long roleId,
            @RequestParam("companyId") Long companyId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {
        requireManagerRole(extractRoleFromAuthorizationHeader(authorizationHeader));
        log.info("EAM GET /roles/{} companyId={} cid={}", roleId, companyId, correlationId(request));
        RoleResponse data = eamRolePermissionService.getRoleById(companyId, roleId);
        return ResponseEntity.ok(
                ApiResponse.successResponse(HttpStatus.OK.value(), "Role fetched successfully", data));
    }

    /** Creates role. */
    @PostMapping
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @RequestParam("companyId") Long companyId,
            @Valid @RequestBody RoleCreateRequest requestBody,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            HttpServletRequest request) {
        requireManagerRole(extractRoleFromAuthorizationHeader(authorizationHeader));
        log.info("EAM POST /roles companyId={} cid={}", companyId, correlationId(request));
        RoleResponse data = eamRolePermissionService.createRole(companyId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.successResponse(HttpStatus.CREATED.value(), "Role created successfully", data));
    }

    /** Returns correlation id. */
    private String correlationId(HttpServletRequest request) {
        String cid = request.getHeader(HeaderConstants.CORRELATION_ID_HEADER);
        return (cid == null || cid.isBlank()) ? "-" : cid;
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

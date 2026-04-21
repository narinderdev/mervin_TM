package com.example.tm.expense.controller;

import com.example.tm.auth.security.TmJwtService;
import com.example.tm.expense.dto.ExpenseRequestDto;
import com.example.tm.expense.dto.ExpenseResponseDto;
import com.example.tm.expense.service.ExpenseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exposes API endpoints for expense controller.
 */
@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final TmJwtService tmJwtService;

    /** Handles create. */
    @PostMapping
    public ResponseEntity<ExpenseResponseDto> create(
            @Valid @RequestBody ExpenseRequestDto requestDto,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(expenseService.create(requestDto, actorRole));
    }

    /** Returns all expenses for manager view. */
    @GetMapping
    public List<ExpenseResponseDto> getAll(
            @RequestParam(value = "status", required = false) String status,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.getAll(actorRole, status);
    }

    /** Returns by user. */
    @GetMapping("/users/{userId}")
    public List<ExpenseResponseDto> getByUserId(
            @PathVariable Long userId,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.getByUserId(userId, actorRole);
    }

    /** Returns by id. */
    @GetMapping("/{id}")
    public ExpenseResponseDto getById(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.getById(id, actorRole);
    }

    /** Handles update. */
    @PutMapping("/{id}")
    public ExpenseResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequestDto requestDto,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.update(id, requestDto, actorRole);
    }

    /** Sends expense to manager for approval. */
    @PostMapping("/{id}/submit")
    public ExpenseResponseDto submit(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.submit(id, actorRole);
    }

    /** Approves expense. */
    @PostMapping("/{id}/approve")
    public ExpenseResponseDto approve(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.approve(id, actorRole);
    }

    /** Sends expense back to technician. */
    @PostMapping("/{id}/send-back")
    public ExpenseResponseDto sendBack(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        return expenseService.sendBack(id, actorRole);
    }

    /** Handles delete. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        String actorRole = extractRoleFromAuthorizationHeader(authorizationHeader);
        expenseService.delete(id, actorRole);
        return ResponseEntity.noContent().build();
    }

    /** Handles extract role from authorization header. */
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
}

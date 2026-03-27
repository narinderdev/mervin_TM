package com.example.tm.auth.controller;

import com.example.tm.auth.dto.InviteTechnicianRequestDto;
import com.example.tm.auth.dto.SetPasswordDto;
import com.example.tm.auth.service.TmInviteService;
import com.example.tm.shared.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Exposes API endpoints for invite controller.
 */
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InviteController {

    private final TmInviteService inviteService;

    /** Handles invite technician. */
    @PostMapping("/technicians")
    public ResponseEntity<ApiResponse<String>> inviteTechnician(@Valid @RequestBody InviteTechnicianRequestDto request) {
        String inviteLink = inviteService.inviteTechnician(request);
        ApiResponse<String> body = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Invitation sent successfully",
                inviteLink);
        return ResponseEntity.ok(body);
    }

    /** Handles accept invite. */
    @GetMapping("/accept")
    public ResponseEntity<Void> acceptInvite(@RequestParam String email) {
        inviteService.validateInvite(email);
        String redirectUrl = inviteService.getSetPasswordRedirectUrl(email);
        return ResponseEntity.status(HttpStatus.FOUND).header("Location", redirectUrl).build();
    }

    /** Sets password. */
    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(@Valid @RequestBody SetPasswordDto dto) {
        inviteService.setPassword(dto);
        ApiResponse<Void> body = ApiResponse.successResponse(
                HttpStatus.OK.value(),
                "Password set successfully",
                null);
        return ResponseEntity.ok(body);
    }
}

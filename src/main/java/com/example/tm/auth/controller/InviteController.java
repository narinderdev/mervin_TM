package com.example.tm.auth.controller;

import com.example.tm.auth.dto.AcceptInviteRequestDto;
import com.example.tm.auth.dto.InviteTechnicianRequestDto;
import com.example.tm.auth.dto.SetPasswordDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.service.TmInviteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InviteController {

    private final TmInviteService inviteService;

    @PostMapping("/technicians")
    public ResponseEntity<String> inviteTechnician(@Valid @RequestBody InviteTechnicianRequestDto request) {
        String token = inviteService.inviteTechnician(request);
        return ResponseEntity.ok(token);
    }

    @GetMapping("/accept")
    public ResponseEntity<Void> acceptInvite(@RequestParam String email) {
        inviteService.validateInvite(email);
        String redirectUrl = inviteService.getSetPasswordRedirectUrl(email);
        return ResponseEntity.status(302).header("Location", redirectUrl).build();
    }

    @PostMapping("/set-password")
    public ResponseEntity<Void> setPassword(@Valid @RequestBody SetPasswordDto dto) {
        inviteService.setPassword(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept")
    public ResponseEntity<UserSummaryDto> acceptInvite(@Valid @RequestBody AcceptInviteRequestDto request) {
        return ResponseEntity.ok(inviteService.acceptInvite(request));
    }
}

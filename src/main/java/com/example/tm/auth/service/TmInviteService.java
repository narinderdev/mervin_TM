package com.example.tm.auth.service;

import com.example.tm.auth.dto.AcceptInviteRequestDto;
import com.example.tm.auth.dto.InviteTechnicianRequestDto;
import com.example.tm.auth.dto.SetPasswordDto;
import com.example.tm.auth.dto.UserSummaryDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.entity.TmUserInvite;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.repository.TmUserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TmInviteService {

    private static final Logger log = LoggerFactory.getLogger(TmInviteService.class);

    private final TmUserInviteRepository inviteRepository;
    private final TmUserRepository tmUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.set-password-url:/set-password}")
    private String setPasswordUrl;

    /**
     * Create an invite for a technician; logs the token so it can be sent via email.
     */
    @Transactional(transactionManager = "tmTransactionManager")
    public String inviteTechnician(InviteTechnicianRequestDto request) {
        String email = normalize(request.getEmail());

        if (tmUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }
        if (inviteRepository.existsByEmailAndAcceptedFalse(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An invite is already pending for this email");
        }

        TmUserInvite invite = new TmUserInvite();
        invite.setFirstName(request.getFirstName().trim());
        invite.setLastName(request.getLastName().trim());
        invite.setEmail(email);
        invite.setRole("Technician");
        invite.setToken(UUID.randomUUID().toString().replace("-", ""));
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        invite.setAccepted(false);
        inviteRepository.save(invite);

        // TODO: send email with link; for now, log token so it can be shared manually.
        log.info("Technician invite created for {} token={}", email, invite.getToken());
        return invite.getToken();
    }

    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public String getSetPasswordRedirectUrl(String email) {
        String encoded = java.net.URLEncoder.encode(normalize(email), java.nio.charset.StandardCharsets.UTF_8);
        String separator = setPasswordUrl.contains("?") ? "&" : "?";
        return setPasswordUrl + separator + "email=" + encoded;
    }

    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public void validateInvite(String email) {
        inviteRepository.findByEmailAndAcceptedFalseAndExpiresAtAfter(normalize(email), Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found or expired"));
    }

    @Transactional(transactionManager = "tmTransactionManager")
    public void setPassword(SetPasswordDto dto) {
        String email = normalize(dto.getEmail());
        TmUserInvite invite = inviteRepository.findByEmailAndAcceptedFalseAndExpiresAtAfter(email, Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found or expired"));

        if (tmUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        TmUser user = new TmUser();
        user.setFirstName(invite.getFirstName());
        user.setLastName(invite.getLastName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole("Technician");
        user.setActive(true);
        tmUserRepository.save(user);

        invite.setAccepted(true);
        inviteRepository.save(invite);
    }

    @Transactional(transactionManager = "tmTransactionManager")
    public UserSummaryDto acceptInvite(AcceptInviteRequestDto request) {
        String token = request.getToken().trim();
        TmUserInvite invite = inviteRepository.findByTokenAndAcceptedFalseAndExpiresAtAfter(token, Instant.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite invalid or expired"));

        if (tmUserRepository.existsByEmailIgnoreCase(invite.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        TmUser user = new TmUser();
        user.setFirstName(invite.getFirstName());
        user.setLastName(invite.getLastName());
        user.setEmail(invite.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole("Technician");
        user.setActive(true);
        TmUser saved = tmUserRepository.save(user);

        invite.setAccepted(true);
        inviteRepository.save(invite);

        return UserSummaryDto.builder()
                .id(saved.getId())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .email(saved.getEmail())
                .role(saved.getRole())
                .active(saved.getActive())
                .build();
    }

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}

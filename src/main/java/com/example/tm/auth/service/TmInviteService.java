package com.example.tm.auth.service;

import com.example.tm.auth.dto.InviteTechnicianRequestDto;
import com.example.tm.auth.dto.SetPasswordDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.entity.TmUserInvite;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.shared.EmailService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.core.io.ClassPathResource;
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
    private final EmailService emailService;

    @Value("${app.frontend.set-password-url:https://d5d0-203-190-154-162.ngrok-free.app/set-password}")
    private String setPasswordUrl;

    @Value("${spring.application.name:tm}")
    private String applicationName;

    @Value("${app.invite.accept-url:https://d5d0-203-190-154-162.ngrok-free.app/api/invitations/accept}")
    private String acceptUrl;

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

        String inviteLink = getSetPasswordRedirectUrl(email);

        // Send email
        String html = renderInviteEmail(invite, inviteLink);
        emailService.sendHtml(email, "You're invited to join " + applicationName, html);

        log.info("Technician invite created for {} link={}", email, inviteLink);
        return inviteLink;
    }

    @Transactional(readOnly = true, transactionManager = "tmTransactionManager")
    public String getSetPasswordRedirectUrl(String email) {
        String encoded = java.net.URLEncoder.encode(normalize(email), java.nio.charset.StandardCharsets.UTF_8);
        String base = normalizeUrl(setPasswordUrl);
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "email=" + encoded;
    }

    private String buildAcceptLink(String email) {
        String encoded = java.net.URLEncoder.encode(normalize(email), java.nio.charset.StandardCharsets.UTF_8);
        String base = normalizeUrl(acceptUrl);
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + "email=" + encoded;
    }

    private String renderInviteEmail(TmUserInvite invite, String inviteLink) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/invite-user.html");
            String template = java.nio.file.Files.readString(resource.getFile().toPath());
            String name = invite.getFirstName() == null ? invite.getEmail() : invite.getFirstName();
            return template
                    .replace("{{name}}", name)
                    .replace("{{companyName}}", applicationName)
                    .replace("{{inviteLink}}", inviteLink);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to load invite template");
        }
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

    private String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeUrl(String url) {
        if (url == null) return "";
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

}

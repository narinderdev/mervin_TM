package com.example.tm.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tm.auth.dto.InviteTechnicianRequestDto;
import com.example.tm.auth.entity.TmUserInvite;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.shared.EmailService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests behavior of tm invite service test.
 */
@ExtendWith(MockitoExtension.class)
class TmInviteServiceTest {

    @Mock
    private TmUserInviteRepository inviteRepository;

    @Mock
    private TmUserRepository tmUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private TmInviteService service;

    @BeforeEach
    void setUp() {
        service = new TmInviteService(inviteRepository, tmUserRepository, passwordEncoder, emailService);
        ReflectionTestUtils.setField(service, "setPasswordUrl", "http://localhost:4200/set-password");
        ReflectionTestUtils.setField(service, "applicationName", "tm");
    }

    @Test
    void inviteTechnician_refreshesExistingInviteInsteadOfFailing() {
        InviteTechnicianRequestDto request = request("Jane", "Doe", "jane@example.com");
        TmUserInvite existing = new TmUserInvite();
        existing.setId(10L);
        existing.setEmail("jane@example.com");
        existing.setAccepted(false);
        existing.setExpiresAt(Instant.now().plusSeconds(60));

        when(tmUserRepository.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(inviteRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(existing));
        when(inviteRepository.save(any(TmUserInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendHtml(eq("jane@example.com"), any(), any());

        String inviteLink = service.inviteTechnician(request);

        assertEquals("http://localhost:4200/set-password?email=jane%40example.com", inviteLink);
        verify(inviteRepository).save(existing);
        verify(emailService).sendHtml(eq("jane@example.com"), any(), any());
        assertEquals(false, existing.getAccepted());
        assertNotNull(existing.getToken());
    }

    @Test
    void inviteTechnician_createsInviteWhenNoExistingRow() {
        InviteTechnicianRequestDto request = request("John", "Smith", "john@example.com");

        when(tmUserRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);
        when(inviteRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(inviteRepository.save(any(TmUserInvite.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(emailService).sendHtml(eq("john@example.com"), any(), any());

        service.inviteTechnician(request);

        ArgumentCaptor<TmUserInvite> captor = ArgumentCaptor.forClass(TmUserInvite.class);
        verify(inviteRepository).save(captor.capture());
        TmUserInvite saved = captor.getValue();
        assertEquals("john@example.com", saved.getEmail());
        assertEquals("John", saved.getFirstName());
        assertEquals("Smith", saved.getLastName());
        assertEquals(false, saved.getAccepted());
        assertNotNull(saved.getToken());
    }

    @Test
    void inviteTechnician_rejectsWhenUserAlreadyExists() {
        InviteTechnicianRequestDto request = request("A", "B", "exists@example.com");
        when(tmUserRepository.existsByEmailIgnoreCase("exists@example.com")).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.inviteTechnician(request));
        verify(inviteRepository, never()).save(any(TmUserInvite.class));
        verify(emailService, never()).sendHtml(any(), any(), any());
    }

    // Tests request.
    private InviteTechnicianRequestDto request(String firstName, String lastName, String email) {
        InviteTechnicianRequestDto dto = new InviteTechnicianRequestDto();
        dto.setFirstName(firstName);
        dto.setLastName(lastName);
        dto.setEmail(email);
        return dto;
    }
}

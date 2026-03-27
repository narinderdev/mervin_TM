package com.example.tm.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.tm.auth.dto.LoginRequestDto;
import com.example.tm.auth.dto.LoginResponseDto;
import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamRole;
import com.example.tm.auth.integration.eam.EamUser;
import com.example.tm.auth.integration.eam.EamUserCompany;
import com.example.tm.auth.integration.eam.EamUserCompanyRepository;
import com.example.tm.auth.integration.eam.EamUserRepository;
import com.example.tm.auth.integration.eam.EamUserRole;
import com.example.tm.auth.integration.eam.EamUserStatus;
import com.example.tm.auth.repository.TmUserInviteRepository;
import com.example.tm.auth.repository.TmUserRepository;
import com.example.tm.auth.security.TmJwtService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tests behavior of tm auth service.
 */
@ExtendWith(MockitoExtension.class)
class TmAuthServiceTest {

    @Mock
    private TmUserRepository tmUserRepository;

    @Mock
    private EamUserRepository eamUserRepository;

    @Mock
    private EamUserCompanyRepository eamUserCompanyRepository;

    @Mock
    private TmUserInviteRepository inviteRepository;

    @Mock
    private TmJwtService tmJwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private TmAuthService service;

    @BeforeEach
    void setUp() {
        service = new TmAuthService(
                tmUserRepository,
                eamUserRepository,
                eamUserCompanyRepository,
                inviteRepository,
                tmJwtService,
                passwordEncoder);
    }

    @Test
    void login_existingTmUser_resyncsFromEam() {
        LoginRequestDto request = loginRequest(" tech@example.com ", "new-password");
        EamUser eamUser = eamUser(7L, "tech@example.com", "eam-new-hash", "Eam", "User", "Admin");
        EamCompany company = company(3L);
        EamUserCompany userCompany = userCompany(company);
        TmUser tmUser = tmUser(42L, "tech@example.com", "tm-old-hash", "Technician", true, "Old", "Name");

        when(eamUserRepository.findByEmailAndDeletedFalse("tech@example.com")).thenReturn(Optional.of(eamUser));
        when(eamUserCompanyRepository.findByUser_IdAndCompany_ActiveTrue(7L)).thenReturn(List.of(userCompany));
        when(passwordEncoder.matches("new-password", "eam-new-hash")).thenReturn(true);
        when(tmUserRepository.findByEmailIgnoreCase("tech@example.com")).thenReturn(Optional.of(tmUser));
        when(tmUserRepository.save(tmUser)).thenReturn(tmUser);
        when(tmJwtService.generateAccessToken(tmUser)).thenReturn("jwt-token");

        LoginResponseDto response = service.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("Eam", tmUser.getFirstName());
        assertEquals("User", tmUser.getLastName());
        assertEquals("tech@example.com", tmUser.getEmail());
        assertEquals("eam-new-hash", tmUser.getPasswordHash());
        assertEquals("Admin", tmUser.getRole());
        verify(passwordEncoder).matches("new-password", "eam-new-hash");
        verify(passwordEncoder, never()).matches("new-password", "tm-old-hash");
        verify(tmUserRepository).save(tmUser);
    }

    @Test
    void login_rejectsWhenEamPasswordDoesNotMatch() {
        LoginRequestDto request = loginRequest("tech@example.com", "old-password");
        EamUser eamUser = eamUser(9L, "tech@example.com", "eam-current-hash", "Eam", "User", "Technician");
        EamCompany company = company(5L);
        EamUserCompany userCompany = userCompany(company);

        when(eamUserRepository.findByEmailAndDeletedFalse("tech@example.com")).thenReturn(Optional.of(eamUser));
        when(eamUserCompanyRepository.findByUser_IdAndCompany_ActiveTrue(9L)).thenReturn(List.of(userCompany));
        when(passwordEncoder.matches("old-password", "eam-current-hash")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.login(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(tmUserRepository, never()).findByEmailIgnoreCase(anyString());
        verify(tmJwtService, never()).generateAccessToken(any(TmUser.class));
    }

    private LoginRequestDto loginRequest(String email, String password) {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private EamUser eamUser(
            Long id,
            String email,
            String passwordHash,
            String firstName,
            String lastName,
            String roleName) {
        EamRole role = new EamRole();
        role.setName(roleName);

        EamUserRole userRole = new EamUserRole();
        userRole.setRole(role);

        EamUser user = new EamUser();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(passwordHash);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setStatus(EamUserStatus.ACTIVE);
        user.setDeleted(false);
        user.setUserRoles(List.of(userRole));
        return user;
    }

    private EamCompany company(Long id) {
        EamCompany company = new EamCompany();
        company.setId(id);
        company.setActive(true);
        company.setCompanyLegalName("Legal");
        company.setCompanyTradeName("Trade");
        company.setCompanyNumber("123");
        company.setAddress("Address");
        company.setCity("City");
        company.setCountry("Country");
        company.setPostalCode("10001");
        return company;
    }

    private EamUserCompany userCompany(EamCompany company) {
        EamUserCompany userCompany = new EamUserCompany();
        userCompany.setCompany(company);
        return userCompany;
    }

    private TmUser tmUser(
            Long id,
            String email,
            String passwordHash,
            String role,
            boolean active,
            String firstName,
            String lastName) {
        TmUser user = new TmUser();
        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(role);
        user.setActive(active);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }
}

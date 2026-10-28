package com.example.tm.auth.security;

import com.example.tm.auth.integration.eam.EamCompany;
import com.example.tm.auth.integration.eam.EamCompanyRepository;
import com.example.tm.auth.integration.eam.EamUser;
import com.example.tm.auth.integration.eam.EamUserCompany;
import com.example.tm.auth.integration.eam.EamUserCompanyRepository;
import com.example.tm.auth.integration.eam.EamUserRepository;
import com.example.tm.auth.integration.eam.EamUserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmCompanyAuthorizationServiceTest {

    private EamUserRepository userRepository;
    private EamUserCompanyRepository userCompanyRepository;
    private EamCompanyRepository companyRepository;
    private JdbcTemplate tmJdbcTemplate;
    private TmCompanyAuthorizationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(EamUserRepository.class);
        userCompanyRepository = mock(EamUserCompanyRepository.class);
        companyRepository = mock(EamCompanyRepository.class);
        tmJdbcTemplate = mock(JdbcTemplate.class);
        service = new TmCompanyAuthorizationService(
                userRepository, userCompanyRepository, companyRepository, tmJdbcTemplate);
    }

    @Test
    void allowsExplicitActiveEamMembership() {
        TmUserPrincipal principal = new TmUserPrincipal(1L, "shared@example.com", "Admin");
        EamCompany company = activeCompany(10L);
        EamUser user = new EamUser();
        user.setId(100L);
        EamUserCompany membership = new EamUserCompany();
        membership.setCompany(company);

        when(companyRepository.findById(10L)).thenReturn(Optional.of(company));
        user.setStatus(EamUserStatus.ACTIVE);
        when(userRepository.findByEmailAndDeletedFalse("shared@example.com")).thenReturn(Optional.of(user));
        when(userCompanyRepository.findByUser_IdAndCompany_ActiveTrue(100L))
                .thenReturn(java.util.List.of(membership));

        assertTrue(service.isActiveMember(principal, 10L));
        verify(tmJdbcTemplate, never()).queryForObject(anyString(), eq(Number.class), eq(10L), anyString());
    }

    @Test
    void rejectsCompanyWithoutEitherMembershipSource() {
        TmUserPrincipal principal = new TmUserPrincipal(1L, "shared@example.com", "Admin");
        EamUser user = new EamUser();
        user.setId(100L);
        user.setStatus(EamUserStatus.ACTIVE);

        when(companyRepository.findById(20L)).thenReturn(Optional.of(activeCompany(20L)));
        when(userRepository.findByEmailAndDeletedFalse("shared@example.com")).thenReturn(Optional.of(user));
        when(userCompanyRepository.findByUser_IdAndCompany_ActiveTrue(100L)).thenReturn(java.util.List.of());
        when(tmJdbcTemplate.queryForObject(anyString(), eq(Number.class), eq(20L), eq("shared@example.com")))
                .thenReturn(0);

        assertFalse(service.isActiveMember(principal, 20L));
    }

    @Test
    void preservesTechnicianEmailCompanyFallback() {
        TmUserPrincipal principal = new TmUserPrincipal(1L, "tech@example.com", "Technician");

        when(companyRepository.findById(30L)).thenReturn(Optional.of(activeCompany(30L)));
        when(userRepository.findByEmailAndDeletedFalse("tech@example.com")).thenReturn(Optional.empty());
        when(tmJdbcTemplate.queryForObject(anyString(), eq(Number.class), eq(30L), eq("tech@example.com")))
                .thenReturn(1);

        assertTrue(service.isActiveMember(principal, 30L));
    }

    @Test
    void doesNotUseTechnicianFallbackForInactiveEamUser() {
        TmUserPrincipal principal = new TmUserPrincipal(1L, "disabled@example.com", "Technician");
        EamUser user = new EamUser();
        user.setId(101L);
        user.setStatus(EamUserStatus.INACTIVE);

        when(companyRepository.findById(30L)).thenReturn(Optional.of(activeCompany(30L)));
        when(userRepository.findByEmailAndDeletedFalse("disabled@example.com")).thenReturn(Optional.of(user));

        assertFalse(service.isActiveMember(principal, 30L));
        verify(tmJdbcTemplate, never())
                .queryForObject(anyString(), eq(Number.class), eq(30L), eq("disabled@example.com"));
    }

    private EamCompany activeCompany(long id) {
        EamCompany company = new EamCompany();
        company.setId(id);
        company.setActive(true);
        return company;
    }
}

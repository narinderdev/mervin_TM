package com.example.tm.eam.service;

import com.example.tm.auth.security.TmCompanyContextHolder;
import com.example.tm.eam.dto.TechnicianTeamCreateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EamLookupServiceTenantTest {

    private JdbcTemplate eamJdbcTemplate;
    private JdbcTemplate tmJdbcTemplate;
    private EamLookupServiceImpl service;

    @BeforeEach
    void setUp() {
        eamJdbcTemplate = mock(JdbcTemplate.class);
        tmJdbcTemplate = mock(JdbcTemplate.class);
        service = new EamLookupServiceImpl(eamJdbcTemplate, tmJdbcTemplate);
        TmCompanyContextHolder.setCompanyId(10L);
    }

    @AfterEach
    void tearDown() {
        TmCompanyContextHolder.clear();
    }

    @Test
    void rejectsAssigningTechnicianFromAnotherCompanyToTeam() {
        when(eamJdbcTemplate.queryForObject(anyString(), eq(Number.class), eq(10L))).thenReturn(1);
        when(tmJdbcTemplate.queryForObject(
                contains("SELECT COUNT(1) FROM technician_teams"), eq(Number.class), eq(10L), eq("Ops")))
                .thenReturn(0);
        when(tmJdbcTemplate.queryForObject(
                contains("INSERT INTO technician_teams"), eq(Long.class),
                eq(10L), eq("Ops"), any(), eq("ACTIVE"), any(), any(), any()))
                .thenReturn(99L);
        when(tmJdbcTemplate.queryForList(
                contains("FROM technician_team_members"), eq(99L)))
                .thenReturn(List.of());
        // Technician 1 belongs to selected company 10. Technician 2 exists only in another company,
        // so the company-scoped lookup deliberately returns only technician 1.
        when(tmJdbcTemplate.queryForList(
                contains("SELECT id FROM technicians WHERE is_deleted = 0 AND company_id = ?"),
                eq(10L), eq(1L), eq(2L)))
                .thenReturn(List.of(Map.of("id", 1L)));

        TechnicianTeamCreateRequest request = new TechnicianTeamCreateRequest();
        request.setCompanyId(20L); // Untrusted body value must not override the request context.
        request.setTeamName("Ops");
        request.setTechnicianIds(List.of(1L, 2L));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createTechnicianTeam(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("One or more technicians were not found", exception.getReason());
    }
}

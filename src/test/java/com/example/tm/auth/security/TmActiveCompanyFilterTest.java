package com.example.tm.auth.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TmActiveCompanyFilterTest {

    private static final TmUserPrincipal PRINCIPAL =
            new TmUserPrincipal(7L, "shared@example.com", "Admin");

    private TmCompanyAuthorizationService companyAuthorizationService;
    private TmSecurityErrorWriter errorWriter;
    private TmActiveCompanyFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        companyAuthorizationService = mock(TmCompanyAuthorizationService.class);
        errorWriter = mock(TmSecurityErrorWriter.class);
        filter = new TmActiveCompanyFilter(companyAuthorizationService, errorWriter);

        doAnswer(invocation -> {
            MockHttpServletResponse response = invocation.getArgument(1);
            HttpStatus status = invocation.getArgument(2);
            response.setStatus(status.value());
            return null;
        }).when(errorWriter).write(any(), any(), any(), any());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(PRINCIPAL, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TmCompanyContextHolder.clear();
    }

    @Test
    void exposesValidatedCompanyToDownstreamCode() throws Exception {
        when(companyAuthorizationService.isActiveMember(PRINCIPAL, 10L)).thenReturn(true);
        MockHttpServletRequest request = request("GET", "/api/technicians", 10L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        FilterChain chain = (req, res) -> {
            invoked.set(true);
            assertEquals(10L, TmCompanyContextHolder.requireCompanyId());
        };

        filter.doFilter(request, response, chain);

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
        verify(companyAuthorizationService).isActiveMember(PRINCIPAL, 10L);
    }

    @Test
    void rejectsMissingCompanyContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/technicians");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(400, response.getStatus());
        verify(errorWriter).write(eq(request), eq(response), eq(HttpStatus.BAD_REQUEST), any());
    }

    @ParameterizedTest(name = "{0} {1} is tenant protected")
    @MethodSource("tenantProtectedRoutes")
    void rejectsCrossTenantTechnicianAndTeamOperations(String method, String path) throws Exception {
        when(companyAuthorizationService.isActiveMember(PRINCIPAL, 20L)).thenReturn(false);
        MockHttpServletRequest request = request(method, path, 20L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(403, response.getStatus());
        verify(companyAuthorizationService).isActiveMember(PRINCIPAL, 20L);
    }

    private static Stream<Arguments> tenantProtectedRoutes() {
        return Stream.of(
                Arguments.of("GET", "/api/technicians"),
                Arguments.of("GET", "/api/technicians/42"),
                Arguments.of("POST", "/api/technicians"),
                Arguments.of("PATCH", "/api/technicians/42"),
                Arguments.of("DELETE", "/api/technicians/42"),
                Arguments.of("GET", "/api/technician-teams"),
                Arguments.of("GET", "/api/technician-teams/5"),
                Arguments.of("POST", "/api/technician-teams"),
                Arguments.of("PATCH", "/api/technician-teams/5"),
                Arguments.of("DELETE", "/api/technician-teams/5")
        );
    }

    private MockHttpServletRequest request(String method, String path, long companyId) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.addParameter("companyId", Long.toString(companyId));
        return request;
    }
}

package com.example.tm.auth.security;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TmJwtAuthenticationFilterTest {

    private TmJwtService jwtService;
    private TmUserRepository userRepository;
    private TmSecurityErrorWriter errorWriter;
    private TmJwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new TmJwtService("test-secret-that-is-at-least-thirty-two-characters", 60_000L);
        userRepository = mock(TmUserRepository.class);
        errorWriter = mock(TmSecurityErrorWriter.class);
        filter = new TmJwtAuthenticationFilter(jwtService, userRepository, errorWriter);
        doAnswer(invocation -> {
            MockHttpServletResponse response = invocation.getArgument(1);
            HttpStatus status = invocation.getArgument(2);
            response.setStatus(status.value());
            return null;
        }).when(errorWriter).write(any(), any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesActiveUserFromAccessToken() throws Exception {
        TmUser user = activeUser();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken(user);
        MockHttpServletRequest request = bearerRequest(token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(request, response, (req, res) -> {
            invoked.set(true);
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            TmUserPrincipal tmPrincipal = assertInstanceOf(TmUserPrincipal.class, principal);
            assertEquals(7L, tmPrincipal.userId());
            assertEquals("shared@example.com", tmPrincipal.email());
        });

        assertTrue(invoked.get());
        assertEquals(200, response.getStatus());
    }

    @Test
    void rejectsNonAccessToken() throws Exception {
        String token = jwtService.generateToken(
                "shared@example.com", Map.of("userId", 7L, "type", "mfa"), 60_000L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(bearerRequest(token), response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsExpiredTokenAsUnauthorized() throws Exception {
        String token = jwtService.generateToken(
                "shared@example.com", Map.of("userId", 7L, "type", "access"), -1_000L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean();

        filter.doFilter(bearerRequest(token), response, (req, res) -> invoked.set(true));

        assertFalse(invoked.get());
        assertEquals(401, response.getStatus());
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/technicians");
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }

    private TmUser activeUser() {
        TmUser user = new TmUser();
        user.setId(7L);
        user.setEmail("shared@example.com");
        user.setRole("Admin");
        user.setActive(true);
        return user;
    }
}

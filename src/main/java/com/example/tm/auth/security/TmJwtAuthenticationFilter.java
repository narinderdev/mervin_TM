package com.example.tm.auth.security;

import com.example.tm.auth.entity.TmUser;
import com.example.tm.auth.repository.TmUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Authenticates TM access tokens and establishes identity only; company is authorized separately. */
@Component
public class TmJwtAuthenticationFilter extends OncePerRequestFilter {

    private final TmJwtService jwtService;
    private final TmUserRepository userRepository;
    private final TmSecurityErrorWriter errorWriter;

    public TmJwtAuthenticationFilter(
            TmJwtService jwtService,
            TmUserRepository userRepository,
            TmSecurityErrorWriter errorWriter) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        try {
            Claims claims = jwtService.parseClaims(token);
            if (!"access".equals(claims.get("type", String.class))) {
                errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid access token");
                return;
            }

            Long userId = numberAsLong(claims.get("userId"));
            String email = normalize(claims.getSubject());
            TmUser user = userId == null ? null : userRepository.findById(userId).orElse(null);
            if (user == null
                    || !Boolean.TRUE.equals(user.getActive())
                    || email == null
                    || !Objects.equals(email, normalize(user.getEmail()))) {
                errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid access token");
                return;
            }

            String role = user.getRole() == null ? "" : user.getRole().trim();
            TmUserPrincipal principal = new TmUserPrincipal(user.getId(), email, role);
            List<SimpleGrantedAuthority> authorities = role.isBlank()
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase().replaceAll("[^A-Z0-9]+", "_")));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid or expired access token");
        }
    }

    private static Long numberAsLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}

package com.example.tm.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Validates the selected company against the authenticated user and binds it to the current request. */
@Component
public class TmActiveCompanyFilter extends OncePerRequestFilter {

    public static final String COMPANY_ID_PARAMETER = "companyId";

    private final TmCompanyAuthorizationService companyAuthorizationService;
    private final TmSecurityErrorWriter errorWriter;

    public TmActiveCompanyFilter(
            TmCompanyAuthorizationService companyAuthorizationService,
            TmSecurityErrorWriter errorWriter) {
        this.companyAuthorizationService = companyAuthorizationService;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        try {
            if (!requiresCompanyContext(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof TmUserPrincipal principal)) {
                filterChain.doFilter(request, response);
                return;
            }

            Long companyId = parseCompanyId(request, response);
            if (companyId == null) {
                return;
            }
            if (!companyAuthorizationService.isActiveMember(principal, companyId)) {
                errorWriter.write(request, response, HttpStatus.FORBIDDEN, "You are not assigned to this company");
                return;
            }

            TmCompanyContextHolder.setCompanyId(companyId);
            filterChain.doFilter(request, response);
        } finally {
            TmCompanyContextHolder.clear();
        }
    }

    static boolean requiresCompanyContext(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        if (path.equals("/auth/users")) {
            return true;
        }
        if (!path.startsWith("/api/")) {
            return false;
        }
        return !path.startsWith("/api/mfa/")
                && !path.equals("/api/invitations/accept")
                && !path.equals("/api/invitations/set-password");
    }

    private Long parseCompanyId(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String[] values = request.getParameterValues(COMPANY_ID_PARAMETER);
        if (values == null || values.length == 0 || values[0] == null || values[0].isBlank()) {
            errorWriter.write(request, response, HttpStatus.BAD_REQUEST, "companyId query parameter is required");
            return null;
        }
        if (Arrays.stream(values).map(value -> value == null ? "" : value.trim()).distinct().count() > 1) {
            errorWriter.write(request, response, HttpStatus.BAD_REQUEST, "companyId must be given exactly once");
            return null;
        }
        try {
            long companyId = Long.parseLong(values[0].trim());
            if (companyId <= 0) {
                throw new NumberFormatException();
            }
            return companyId;
        } catch (NumberFormatException ex) {
            errorWriter.write(request, response, HttpStatus.BAD_REQUEST, "companyId must be a positive number");
            return null;
        }
    }
}

package com.example.tm.shared.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int REQUEST_CACHE_LIMIT_BYTES = 1024 * 1024;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator")
                || path.startsWith("/webjars")
                || path.startsWith("/favicon");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper =
                new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT_BYTES);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTimeMs = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long durationMs = System.currentTimeMillis() - startTimeMs;
            logRequest(requestWrapper);
            logResponse(responseWrapper, durationMs);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String fullUrl = query == null ? uri : uri + "?" + query;

        StringBuilder sb = new StringBuilder();
        sb.append("\n--- [TM HTTP REQUEST] -----------------------------\n");
        sb.append(method).append(" ").append(fullUrl).append("\n");

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            String value = request.getHeader(name);
            sb.append("Header: ").append(name).append(" = ").append(maskHeader(name, value)).append("\n");
        }

        String contentType = request.getContentType();
        boolean multipart = contentType != null && contentType.startsWith("multipart/");
        if (multipart) {
            sb.append("Body: [multipart content omitted]\n");
        } else {
            String body = getBody(request.getContentAsByteArray());
            if (!body.isBlank()) {
                sb.append("Body: ").append(maskSensitiveBody(body)).append("\n");
            }
        }

        sb.append("---------------------------------------------------");
        log.info(sb.toString());
    }

    private void logResponse(ContentCachingResponseWrapper response, long durationMs) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n--- [TM HTTP RESPONSE] ----------------------------\n");
        sb.append("Status: ").append(response.getStatus()).append("\n");
        sb.append("Duration: ").append(durationMs).append(" ms\n");

        for (String headerName : response.getHeaderNames()) {
            sb.append("Header: ").append(headerName)
                    .append(" = ").append(maskHeader(headerName, response.getHeader(headerName)))
                    .append("\n");
        }

        String contentType = response.getContentType();
        boolean binary = contentType != null && contentType.startsWith("application/octet-stream");
        if (binary) {
            sb.append("Body: [binary content omitted]\n");
        } else {
            String body = getBody(response.getContentAsByteArray());
            if (!body.isBlank()) {
                sb.append("Body: ").append(maskSensitiveBody(body)).append("\n");
            }
        }

        sb.append("---------------------------------------------------");
        log.info(sb.toString());
    }

    private String getBody(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        return new String(data, StandardCharsets.UTF_8);
    }

    private String maskHeader(String name, String value) {
        if (value == null) {
            return null;
        }
        if ("authorization".equalsIgnoreCase(name)) {
            int separator = value.indexOf(' ');
            if (separator > 0) {
                return value.substring(0, separator) + " ***";
            }
            return "***";
        }
        return value;
    }

    private String maskSensitiveBody(String body) {
        return body
                .replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***\"")
                .replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***\"")
                .replaceAll("\"accessToken\"\\s*:\\s*\"[^\"]*\"", "\"accessToken\":\"***\"")
                .replaceAll("\"refreshToken\"\\s*:\\s*\"[^\"]*\"", "\"refreshToken\":\"***\"")
                .replaceAll("\"mfaToken\"\\s*:\\s*\"[^\"]*\"", "\"mfaToken\":\"***\"")
                .replaceAll("\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***\"")
                .replaceAll("\"otp\"\\s*:\\s*\"\\d{4,8}\"", "\"otp\":\"***\"")
                .replaceAll("\"code\"\\s*:\\s*\"\\d{4,8}\"", "\"code\":\"***\"");
    }
}

package com.example.tm.shared.web;

import java.time.Instant;

/**
 * Represents api error response.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        String correlationId
) {
}

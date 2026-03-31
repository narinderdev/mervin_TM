package com.example.tm.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Contains business logic for mfa rate limit service.
 */
@Service
public class MfaRateLimitService {

    private final int maxAttempts;
    private final Duration window;
    private final ConcurrentMap<String, RateLimitState> states = new ConcurrentHashMap<>();

    /** Creates a new instance of mfa rate limit service. */
    public MfaRateLimitService(
            @Value("${app.mfa.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${app.mfa.rate-limit.window-seconds:300}") long windowSeconds) {
        this.maxAttempts = maxAttempts;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    /** Handles check or throw. */
    public void checkOrThrow(String key) {
        if (!tryConsume(key)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many MFA attempts. Try again later.");
        }
    }

    /** Handles try consume. */
    private boolean tryConsume(String key) {
        Instant now = Instant.now();
        AtomicBoolean allowed = new AtomicBoolean(true);
        states.compute(key, (k, state) -> {
            if (state == null || state.windowStart.plus(window).isBefore(now)) {
                return new RateLimitState(1, now);
            }
            if (state.count >= maxAttempts) {
                allowed.set(false);
                return state;
            }
            return new RateLimitState(state.count + 1, state.windowStart);
        });
        return allowed.get();
    }

    private record RateLimitState(int count, Instant windowStart) {
    }
}

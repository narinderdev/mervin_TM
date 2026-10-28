package com.example.tm.auth.security;

/** Authenticated TM identity established from a validated access token. */
public record TmUserPrincipal(Long userId, String email, String role) {
}

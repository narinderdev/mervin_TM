package com.example.tm.auth.security;

import com.example.tm.auth.entity.TmUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Contains business logic for tm jwt service.
 */
@Service
public class TmJwtService {

    private final Key key;
    private final long expirationMs;
    private final ConcurrentMap<String, Long> revokedTokenHashToExpiryMs = new ConcurrentHashMap<>();

    /** Creates a new instance of tm jwt service. */
    public TmJwtService(
            @Value("${app.jwt.secret:TM_Default_Change_Me_At_Least_32_Chars_Long_Secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Handles generate access token. */
    public String generateAccessToken(TmUser user) {
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "roles", List.of(user.getRole()),
                "email", user.getEmail(),
                "type", "access"
        );
        return generateToken(user.getEmail(), claims, expirationMs);
    }

    /** Handles generate token. */
    public String generateToken(String subject, Map<String, Object> claims, long tokenExpirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + tokenExpirationMs);
        Map<String, Object> allClaims = claims == null ? new HashMap<>() : new HashMap<>(claims);
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(allClaims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Handles parse claims. */
    public Claims parseClaims(String token) {
        ensureTokenNotRevoked(token);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            cleanupRevokedTokens();
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid access token", ex);
        }
    }

    /** Handles extract primary role. */
    public String extractPrimaryRole(String token) {
        Claims claims = parseClaims(token);
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List<?> roles && !roles.isEmpty() && roles.get(0) != null) {
            return roles.get(0).toString();
        }
        throw new IllegalArgumentException("Role claim not present in token");
    }

    /** Handles extract email. */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Email claim not present in token");
        }
        return subject.trim().toLowerCase();
    }

    /** Revokes a JWT token until its expiration time. */
    public void revokeToken(String token, Date expiresAt) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing access token");
        }
        long expiryMs = expiresAt == null ? System.currentTimeMillis() : expiresAt.getTime();
        revokedTokenHashToExpiryMs.put(hashToken(token), expiryMs);
        cleanupRevokedTokens();
    }

    /** Ensures revoked access tokens are rejected. */
    private void ensureTokenNotRevoked(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Missing access token");
        }
        String tokenHash = hashToken(token);
        Long expiryMs = revokedTokenHashToExpiryMs.get(tokenHash);
        if (expiryMs == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (expiryMs <= now) {
            revokedTokenHashToExpiryMs.remove(tokenHash, expiryMs);
            return;
        }
        throw new IllegalArgumentException("Access token has been revoked");
    }

    /** Removes expired revoked-token entries. */
    private void cleanupRevokedTokens() {
        long now = System.currentTimeMillis();
        revokedTokenHashToExpiryMs.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    /** Creates a stable hash so raw JWT values are not stored in-memory. */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}

package com.example.tm.auth.security;

import com.example.tm.auth.entity.TmUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TmJwtService {

    private final Key key;
    private final long expirationMs;

    public TmJwtService(
            @Value("${app.jwt.secret:TM_Default_Change_Me_At_Least_32_Chars_Long_Secret}") String secret,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateAccessToken(TmUser user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        Map<String, Object> claims = Map.of(
                "userId", user.getId(),
                "roles", List.of(user.getRole()),
                "email", user.getEmail(),
                "type", "access"
        );

        return Jwts.builder()
                .setSubject(user.getEmail())
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

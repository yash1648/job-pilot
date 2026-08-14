package com.jobpilot.security.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Short-lived access JWT issuance + validation (doc 22 §1).
 * HS256 with a configured secret; claims: sub = userId, email.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(
            @Value("${jobpilot.security.jwt.secret}") String secret,
            @Value("${jobpilot.security.jwt.access-ttl-minutes:15}") long accessTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMinutes(accessTtlMinutes);
    }

    public String issueAccessToken(UUID userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /** Access token lifetime in seconds (for the login/refresh response). */
    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }

    /**
     * @return the userId claim, or {@code null} if invalid/expired/unsigned.
     */
    public UUID parseUserId(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }
}

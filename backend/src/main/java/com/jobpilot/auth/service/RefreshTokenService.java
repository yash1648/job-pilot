package com.jobpilot.auth.service;

import com.jobpilot.auth.domain.RefreshToken;
import com.jobpilot.auth.repository.RefreshTokenRepository;
import com.jobpilot.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh token lifecycle (doc 22 §1): opaque random token stored SHA-256
 * hashed, single-use rotation, per-session revocation ("log out everywhere").
 */
@Service
public class RefreshTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration refreshTtl;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jobpilot.security.jwt.refresh-ttl-days:30}") long refreshTtlDays) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTtl = Duration.ofDays(refreshTtlDays);
    }

    public record IssuedToken(String rawToken, RefreshToken entity) {
    }

    @Transactional
    public IssuedToken issue(UUID userId) {
        String raw = generateRawToken();
        RefreshToken entity = new RefreshToken(
                UUID.randomUUID(), userId, hash(raw), Instant.now().plus(refreshTtl));
        return new IssuedToken(raw, refreshTokenRepository.save(entity));
    }

    /**
     * Validates + rotates (single-use): the presented token must be active;
     * it is revoked and a new token issued in its place.
     */
    @Transactional
    public IssuedToken rotate(String rawToken) {
        RefreshToken current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException("INVALID_REFRESH_TOKEN",
                        HttpStatus.UNAUTHORIZED, "refresh token is invalid"));
        if (!current.isActive()) {
            throw new ApiException("INVALID_REFRESH_TOKEN",
                    HttpStatus.UNAUTHORIZED, "refresh token is expired or revoked");
        }
        IssuedToken next = issue(current.getUserId());
        current.revoke(next.entity().getTokenHash());
        refreshTokenRepository.save(current);
        return next;
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(entity -> {
            if (entity.isActive()) {
                entity.revoke(null);
                refreshTokenRepository.save(entity);
            }
        });
    }

    /** Revokes every active session for a user ("log out everywhere", doc 22 §1). */
    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.findByUserIdAndRevokedAtIsNull(userId).forEach(t -> {
            t.revoke(null);
            refreshTokenRepository.save(t);
        });
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

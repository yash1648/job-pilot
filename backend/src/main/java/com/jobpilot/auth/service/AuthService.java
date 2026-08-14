package com.jobpilot.auth.service;

import com.jobpilot.auth.service.RefreshTokenService.IssuedToken;
import com.jobpilot.common.exception.ApiException;
import com.jobpilot.security.auth.JwtService;
import com.jobpilot.user.service.UserProfile;
import com.jobpilot.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Registration, login, logout, refresh (doc 05 §1, doc 22 §1).
 * Talks to `user` module only through {@link UserService} (doc 34 §3).
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserService userService, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public record TokenPair(String accessToken, String refreshToken, long expiresInSeconds) {
    }

    @Transactional
    public UserProfile register(String email, String rawPassword) {
        return userService.register(email, rawPassword);
    }

    @Transactional
    public TokenPair login(String email, String rawPassword) {
        UserProfile user = userService.authenticate(email, rawPassword);
        return issuePair(user.id());
    }

    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        IssuedToken rotated = refreshTokenService.rotate(rawRefreshToken);
        String accessToken = jwtService.issueAccessToken(rotated.entity().getUserId(),
                userService.findById(rotated.entity().getUserId()).email());
        return new TokenPair(accessToken, rotated.rawToken(), jwtService.accessTtlSeconds());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    public UserProfile me(UUID userId) {
        return userService.findById(userId);
    }

    private TokenPair issuePair(UUID userId) {
        UserProfile user = userService.findById(userId);
        String accessToken = jwtService.issueAccessToken(userId, user.email());
        IssuedToken refresh = refreshTokenService.issue(userId);
        return new TokenPair(accessToken, refresh.rawToken(), jwtService.accessTtlSeconds());
    }
}

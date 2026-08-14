package com.jobpilot.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response DTOs for the auth endpoints (doc 05 §1).
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 128) String password) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record RefreshRequest(
            @NotBlank String refreshToken) {
    }

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            String tokenType) {
        public AuthResponse(String accessToken, String refreshToken, long expiresInSeconds) {
            this(accessToken, refreshToken, expiresInSeconds, "Bearer");
        }
    }

    public record RegisterResponse(String id, String email) {
    }
}

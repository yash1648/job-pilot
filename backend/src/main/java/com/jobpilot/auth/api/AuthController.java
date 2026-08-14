package com.jobpilot.auth.api;

import com.jobpilot.auth.api.AuthDtos.AuthResponse;
import com.jobpilot.auth.api.AuthDtos.LoginRequest;
import com.jobpilot.auth.api.AuthDtos.RefreshRequest;
import com.jobpilot.auth.api.AuthDtos.RegisterRequest;
import com.jobpilot.auth.api.AuthDtos.RegisterResponse;
import com.jobpilot.auth.service.AuthService;
import com.jobpilot.auth.service.AuthService.TokenPair;
import com.jobpilot.user.service.UserProfile;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Auth endpoints (doc 05 §1).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserProfile user = authService.register(request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(user.id().toString(), user.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair pair = authService.login(request.email(), request.password());
        return ResponseEntity.ok(new AuthResponse(
                pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPair pair = authService.refresh(request.refreshToken());
        return ResponseEntity.ok(new AuthResponse(
                pair.accessToken(), pair.refreshToken(), pair.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.noContent().build();
    }

    /** Current authenticated user (round-trip + session proof). */
    @GetMapping("/me")
    public ResponseEntity<RegisterResponse> me(java.security.Principal principal) {
        UserProfile user = authService.me(UUID.fromString(principal.getName()));
        return ResponseEntity.ok(new RegisterResponse(user.id().toString(), user.email()));
    }

    /**
     * OAuth completion (doc 05 §1). Placeholder — OAuth providers are not
     * configured in this release (doc 22 §7, future tier).
     */
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<Void> oauthCallback(@PathVariable String provider) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}

package com.jobpilot.user.service;

import com.jobpilot.common.exception.ApiException;
import com.jobpilot.user.domain.User;
import com.jobpilot.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * User account operations (doc 02 §3: `user` module, depends on `common`).
 * Cross-module access goes through this service interface only (doc 34 §3) —
 * consumers receive {@link UserProfile} DTOs, never the domain entity.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserProfile register(String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("EMAIL_ALREADY_REGISTERED", HttpStatus.CONFLICT,
                    "an account with this email already exists");
        }
        User user = new User(UUID.randomUUID(), email, passwordEncoder.encode(rawPassword));
        return toProfile(userRepository.save(user));
    }

    /**
     * Verifies credentials and marks the login. Fails uniformly for unknown
     * email, wrong password, or non-ACTIVE account (doc 22 §1, §5).
     */
    @Transactional
    public UserProfile authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> invalidCredentials());
        if (user.getStatus() != User.Status.ACTIVE
                || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw invalidCredentials();
        }
        user.markLoggedIn();
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public UserProfile findById(UUID id) {
        return userRepository.findById(id)
                .map(this::toProfile)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND, "user not found"));
    }

    private UserProfile toProfile(User user) {
        return new UserProfile(user.getId(), user.getEmail(), user.getStatus() == User.Status.ACTIVE);
    }

    private ApiException invalidCredentials() {
        return new ApiException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED,
                "email or password is incorrect");
    }
}

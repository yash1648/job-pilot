package com.jobpilot.user.service;

import java.util.UUID;

/**
 * Read-only view of a user for cross-module consumers (doc 34 §3 — modules
 * interact through service interfaces and DTOs, never domain entities).
 */
public record UserProfile(UUID id, String email, boolean active) {
}

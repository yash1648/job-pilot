package com.jobpilot.audit.domain;

/**
 * Who/what produced an {@link AuditEvent} (doc 04 §2.6). Mirrors the
 * {@code actor_type} CHECK constraint on {@code audit_events}.
 */
public enum ActorType {
    USER,
    SYSTEM,
    AI_AGENT
}

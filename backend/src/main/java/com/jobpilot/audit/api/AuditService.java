package com.jobpilot.audit.api;

import com.jobpilot.audit.api.AuditDtos.AuditEventRequest;
import com.jobpilot.audit.api.AuditDtos.AuditTrailResponse;

import java.util.List;
import java.util.UUID;

/**
 * Records security-relevant events and exposes a user's own audit trail
 * (doc 22 §10, doc 05 §9). Cross-module callers depend on this interface only —
 * never on {@code audit.repository} / {@code audit.domain} (doc 34 §3).
 */
public interface AuditService {

    /** Append an audit event. Rejects payloads containing secrets (doc 29 §1). */
    void record(AuditEventRequest request);

    /** The authenticated user's own audit trail, newest first. */
    AuditTrailResponse getForUser(UUID userId);
}

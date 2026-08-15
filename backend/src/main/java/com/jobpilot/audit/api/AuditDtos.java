package com.jobpilot.audit.api;

import com.jobpilot.audit.domain.ActorType;
import com.jobpilot.audit.domain.AuditEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Request/response DTOs for the audit module (doc 05 §9, doc 22 §10).
 */
public final class AuditDtos {

    private AuditDtos() {
    }

    /** System-side call to record an audit event. Never include secrets (doc 29 §1). */
    public record AuditEventRequest(
            ActorType actorType,
            String actorId,
            String eventType,
            String entityType,
            String entityId,
            Map<String, Object> payload) {
    }

    public record AuditEventResponse(
            Long id,
            String actorType,
            String actorId,
            String eventType,
            String entityType,
            String entityId,
            Map<String, Object> payload,
            Instant occurredAt) {

        public static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(
                    e.getId(),
                    e.getActorType().name(),
                    e.getActorId(),
                    e.getEventType(),
                    e.getEntityType(),
                    e.getEntityId(),
                    e.getPayload(),
                    e.getOccurredAt());
        }
    }

    /** User-visible trail wrapper (doc 05 §9). */
    public record AuditTrailResponse(List<AuditEventResponse> events) {
        public static AuditTrailResponse of(List<AuditEvent> events) {
            return new AuditTrailResponse(events.stream().map(AuditEventResponse::from).toList());
        }
    }
}

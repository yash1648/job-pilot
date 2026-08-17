package com.jobpilot.audit.service;

import com.jobpilot.audit.api.AuditDtos.AuditEventRequest;
import com.jobpilot.audit.api.AuditDtos.AuditEventResponse;
import com.jobpilot.audit.api.AuditDtos.AuditTrailResponse;
import com.jobpilot.audit.api.AuditService;
import com.jobpilot.audit.domain.AuditEvent;
import com.jobpilot.audit.domain.ActorType;
import com.jobpilot.audit.repository.AuditEventRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Default {@link AuditService} (doc 22 §10, doc 29 §1). The {@code payload} is
 * scanned for secret-named fields before insert so callers can never
 * accidentally persist credentials/tokens (fail-closed).
 */
@Service
public class AuditServiceImpl implements AuditService {

    // Substrings that indicate a secret-bearing field (doc 29 §1). Deliberately
    // conservative — a hit rejects the whole event rather than silently logging it.
    private static final Set<String> SECRET_KEY_HINTS = Set.of(
            "password", "passwd", "pwd", "token", "secret", "authorization",
            "cookie", "apikey", "api_key", "privatekey", "private_key",
            "credential", "credentials", "accesstoken", "refreshtoken", "otp");

    private static final int MAX_TRAIL = 100;

    private final AuditEventRepository repository;

    public AuditServiceImpl(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(AuditEventRequest request) {
        rejectSecrets(request.payload());
        repository.save(new AuditEvent(
                parseActorType(request.actorType()),
                request.actorId(),
                request.eventType(),
                request.entityType(),
                request.entityId(),
                request.payload()));
    }

    @Override
    public AuditTrailResponse getForUser(UUID userId) {
        List<AuditEventResponse> events = repository
                .findByActorId(userId.toString(), PageRequest.of(0, MAX_TRAIL))
                .stream()
                .map(AuditEventResponse::from)
                .toList();
        return new AuditTrailResponse(events);
    }

    private ActorType parseActorType(String raw) {
        try {
            return ActorType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("unknown actorType: " + raw, e);
        }
    }

    private void rejectSecrets(Map<String, Object> payload) {
        if (payload == null) {
            return;
        }
        scanKeys(payload);
    }

    @SuppressWarnings("unchecked")
    private void scanKeys(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            for (String hint : SECRET_KEY_HINTS) {
                if (key.contains(hint)) {
                    throw new IllegalArgumentException(
                            "audit payload must not contain secrets (field: " + entry.getKey() + ")");
                }
            }
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested) {
                scanKeys((Map<String, Object>) nested);
            } else if (value instanceof Iterable<?> items) {
                for (Object item : items) {
                    if (item instanceof Map<?, ?> nested) {
                        scanKeys((Map<String, Object>) nested);
                    }
                }
            }
        }
    }
}

package com.jobpilot.audit.service;

import com.jobpilot.audit.api.AuditDtos.AuditEventRequest;
import com.jobpilot.audit.api.AuditService;
import com.jobpilot.audit.domain.ActorType;
import com.jobpilot.audit.domain.AuditEvent;
import com.jobpilot.audit.repository.AuditEventRepository;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit test for {@link AuditServiceImpl}: secret-payload rejection (doc 29 §1)
 * and event persistence. No Spring context.
 */
class AuditServiceImplTest {

    private final AuditEventRepository repository = mock(AuditEventRepository.class);
    private final AuditServiceImpl service = new AuditServiceImpl(repository);

    @Test
    void recordsNonSecretEvent() {
        service.record(new AuditEventRequest(
                ActorType.USER, "u1", "LOGIN", "user", "u1", Map.of("ip", "1.2.3.4")));
        verify(repository).save(any(AuditEvent.class));
    }

    @Test
    void rejectsSecretInTopLevelPayload() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.record(new AuditEventRequest(
                        ActorType.USER, "u1", "LOGIN", "user", "u1",
                        Map.of("password", "hunter2"))));
        assertEquals("audit payload must not contain secrets (field: password)", ex.getMessage());
    }

    @Test
    void rejectsSecretInNestedPayload() {
        assertThrows(IllegalArgumentException.class, () ->
                service.record(new AuditEventRequest(
                        ActorType.USER, "u1", "LOGIN", "user", "u1",
                        Map.of("meta", Map.of("sessionToken", "abc")))));
    }

    @Test
    void nullPayloadIsAllowed() {
        service.record(new AuditEventRequest(
                ActorType.SYSTEM, null, "SCHEDULED", "job", "j1", null));
        verify(repository).save(any(AuditEvent.class));
    }
}

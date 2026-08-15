package com.jobpilot.audit.repository;

import com.jobpilot.audit.domain.AuditEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    /** User-visible trail: only events the actor themselves produced (doc 22 §10). */
    @Query("SELECT a FROM AuditEvent a WHERE a.actorId = :actorId ORDER BY a.occurredAt DESC")
    List<AuditEvent> findByActorId(String actorId, Pageable pageable);

    List<AuditEvent> findByEntityTypeAndEntityId(String entityType, String entityId);
}

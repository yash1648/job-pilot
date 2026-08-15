package com.jobpilot.audit.api;

import com.jobpilot.audit.api.AuditDtos.AuditTrailResponse;
import com.jobpilot.audit.api.AuditService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * User-visible audit trail (doc 05 §9, doc 22 §10). Scoped to the authenticated
 * user — a caller only ever sees events they produced.
 */
@RestController
@RequestMapping("/api/v1/settings")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/audit")
    public ResponseEntity<AuditTrailResponse> get(Principal principal) {
        return ResponseEntity.ok(auditService.getForUser(UUID.fromString(principal.getName())));
    }
}

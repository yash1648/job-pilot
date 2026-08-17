package com.jobpilot.candidate.resume.service;

/**
 * Resume AI extraction could not produce schema-valid, evidence-grounded
 * entities (doc 07 §9, doc 06 §6). Carries a user-facing reason; the caller
 * records it as {@code parse_status=FAILED} and emits a {@code ResumeParsingFailed}
 * audit event (doc 07:102).
 */
public class ResumeExtractionException extends RuntimeException {

    private final String reason;

    public ResumeExtractionException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public ResumeExtractionException(String reason, Throwable cause) {
        super(reason, cause);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}
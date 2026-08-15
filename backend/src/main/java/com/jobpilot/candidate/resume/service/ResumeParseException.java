package com.jobpilot.candidate.resume.service;

/**
 * A resume that could not be parsed, carrying a user-facing reason
 * (doc 07:36-38, doc 07 §9). The caller records it as
 * {@code parse_status=FAILED} with this reason.
 */
public class ResumeParseException extends RuntimeException {

    private final String reason;

    public ResumeParseException(String reason) {
        super(reason);
        this.reason = reason;
    }

    public String reason() {
        return reason;
    }
}

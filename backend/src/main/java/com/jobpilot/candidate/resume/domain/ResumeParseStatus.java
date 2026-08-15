package com.jobpilot.candidate.resume.domain;

/**
 * Resume parse lifecycle (doc 03 §2, doc 07 §2). Set to PENDING on upload;
 * later stages (doc 07) move it to PARSED or FAILED. Never silently left
 * empty (doc 07:36-38).
 */
public enum ResumeParseStatus {
    PENDING,
    PARSED,
    FAILED
}

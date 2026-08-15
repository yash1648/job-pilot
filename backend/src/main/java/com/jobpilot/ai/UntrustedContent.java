package com.jobpilot.ai;

/**
 * Marker type for untrusted model input (resume/job/page text, doc 23 §1).
 * The marker exists so prompt construction can never accidentally
 * string-concatenate untrusted content into the trusted instruction channel
 * (doc 23 §2) — it must be passed as a distinct field/role.
 */
public record UntrustedContent(String value) {
}

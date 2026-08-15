package com.jobpilot.ai;

/**
 * Lightweight output contract (doc 06 §6). Declares the field paths that must
 * be present in the parsed model response; schema-invalid output is retried
 * once, then surfaced as {@link AiOutputInvalidException}.
 */
public record ResponseSchema(String[] requiredFields) {
}

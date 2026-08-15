package com.jobpilot.ai;

/**
 * Parsed + validated model output plus raw metadata for observability
 * (doc 06 §1, doc 29).
 */
public record StructuredResponse<T>(
        T data,
        String modelUsed,
        int promptTokens,
        int completionTokens,
        long latencyMs) {
}

package com.jobpilot.common.exception;

/**
 * API error envelope (doc 05:13): {@code { "error": { "code", "message", "details" } }}.
 */
public record ApiError(String code, String message, Object details) {
}

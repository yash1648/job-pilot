package com.jobpilot.ai;

/** Provider (Ollama) down or call timed out — caller applies its own fallback (doc 06 §3/§7). */
public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

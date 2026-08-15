package com.jobpilot.ai;

/** Model output failed schema validation after the bounded retry (doc 06 §6). */
public class AiOutputInvalidException extends RuntimeException {
    public AiOutputInvalidException(String message) {
        super(message);
    }
}

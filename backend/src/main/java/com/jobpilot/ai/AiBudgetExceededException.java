package com.jobpilot.ai;

/** Context or rate budget exceeded before the call was made (doc 06 §3). */
public class AiBudgetExceededException extends RuntimeException {
    public AiBudgetExceededException(String message) {
        super(message);
    }
}

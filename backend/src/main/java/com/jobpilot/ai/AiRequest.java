package com.jobpilot.ai;

/**
 * A model request (doc 06 §1). Carries the task type (drives routing), the
 * untrusted payload wrapped in {@link UntrustedContent}, the trusted system
 * instruction, an optional output schema, and an optional token budget.
 */
public record AiRequest(
        AiTaskType taskType,
        UntrustedContent content,
        String systemInstruction,
        ResponseSchema outputSchema,
        Integer budgetTokens) {

    public AiRequest(AiTaskType taskType, UntrustedContent content, String systemInstruction) {
        this(taskType, content, systemInstruction, null, null);
    }

    public AiRequest(AiTaskType taskType, UntrustedContent content, String systemInstruction, ResponseSchema outputSchema) {
        this(taskType, content, systemInstruction, outputSchema, null);
    }
}

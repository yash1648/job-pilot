package com.jobpilot.ai;

/**
 * AI task types (doc 06 §2). Each maps to a {@link ModelProfile} that drives
 * model routing. Adding a task type never requires touching a provider impl.
 */
public enum AiTaskType {
    SKILL_CLASSIFICATION(ModelProfile.FAST),
    FIELD_MAPPING(ModelProfile.FAST),
    SIMPLE_EXTRACTION(ModelProfile.FAST),
    RESUME_REASONING(ModelProfile.STRONG),
    JOB_ANALYSIS(ModelProfile.STRONG),
    APPLICATION_STRATEGY(ModelProfile.STRONG),
    ANSWER_GENERATION(ModelProfile.STRONG),
    SEMANTIC_EMBEDDING(ModelProfile.EMBEDDING),
    PAGE_UNDERSTANDING(ModelProfile.VISION);

    private final ModelProfile profile;

    AiTaskType(ModelProfile profile) {
        this.profile = profile;
    }

    public ModelProfile profile() {
        return profile;
    }
}

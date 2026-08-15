package com.jobpilot.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Maps {@link AiTaskType} → {@link ModelProfile} → concrete model name
 * (doc 06 §2). Profile→model mapping is configuration-driven so swapping the
 * "strong" model needs no code change.
 */
@Component
public class ModelRouter {

    private final java.util.Map<ModelProfile, String> profileModels;

    public ModelRouter(
            @Value("${jobpilot.ai.ollama.models.fast:llama3.2:3b}") String fast,
            @Value("${jobpilot.ai.ollama.models.strong:llama3.1:8b}") String strong,
            @Value("${jobpilot.ai.ollama.models.embedding:nomic-embed-text}") String embedding,
            @Value("${jobpilot.ai.ollama.models.vision:llava:7b}") String vision) {
        this.profileModels = java.util.Map.of(
                ModelProfile.FAST, fast,
                ModelProfile.STRONG, strong,
                ModelProfile.EMBEDDING, embedding,
                ModelProfile.VISION, vision);
    }

    /** Concrete model name for the task's routed profile. */
    public String resolve(AiTaskType taskType) {
        return profileModels.get(taskType.profile());
    }

    public String resolve(ModelProfile profile) {
        return profileModels.get(profile);
    }
}

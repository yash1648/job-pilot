package com.jobpilot.ai.provider.fake;

import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.StructuredResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deterministic stand-in for {@link AiService} used in tests and local dev
 * without a model (doc 26 §3). Returns canned, schema-shaped output so the
 * pipeline can be exercised end-to-end without a real provider.
 */
public class FakeAiService implements AiService {

    @Override
    public StructuredResponse<?> complete(AiRequest request) {
        Map<String, Object> data = new LinkedHashMap<>();
        switch (request.taskType()) {
            case SKILL_CLASSIFICATION -> data.put("skills", new String[]{"java", "spring-boot", "postgresql"});
            case FIELD_MAPPING, SIMPLE_EXTRACTION -> data.put("fields", Map.of("title", "Backend Engineer"));
            case RESUME_REASONING, JOB_ANALYSIS, APPLICATION_STRATEGY, ANSWER_GENERATION ->
                    data.put("summary", "fake-model-response");
            default -> data.put("result", "ok");
        }
        return new StructuredResponse<>(data, "fake", 1, 1, 0L);
    }
}

package com.jobpilot.ai.provider.fake;

import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.StructuredResponse;

import java.util.LinkedHashMap;
import java.util.List;
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
            case FIELD_MAPPING -> data.put("fields", Map.of("title", "Backend Engineer"));
            case SIMPLE_EXTRACTION, RESUME_REASONING -> data.putAll(extractionShape());
            case JOB_ANALYSIS, APPLICATION_STRATEGY, ANSWER_GENERATION ->
                    data.put("summary", "fake-model-response");
            default -> data.put("result", "ok");
        }
        return new StructuredResponse<>(data, "fake", 1, 1, 0L);
    }

    /**
     * Canned evidence-extraction output (doc 07 §2): one experience, one
     * education, one project — each with a non-empty rawSourceExcerpt so the
     * extraction pipeline can be exercised end-to-end with the fake provider.
     */
    private static Map<String, Object> extractionShape() {
        Map<String, Object> experience = new LinkedHashMap<>();
        experience.put("title", "Backend Engineer");
        experience.put("organization", "Acme Corp");
        experience.put("startDate", "2021-01");
        experience.put("endDate", null);
        experience.put("description", "Built Java microservices on Spring Boot.");
        experience.put("extractedSkills", List.of("java", "spring-boot"));
        experience.put("rawSourceExcerpt", "Backend Engineer at Acme Corp — Java, Spring Boot");

        Map<String, Object> education = new LinkedHashMap<>();
        education.put("title", "BSc Computer Science");
        education.put("organization", "State University");
        education.put("startDate", "2015-09");
        education.put("endDate", "2019-06");
        education.put("description", "Computer science degree.");
        education.put("extractedSkills", List.of("algorithms"));
        education.put("rawSourceExcerpt", "BSc Computer Science, State University 2015-2019");

        Map<String, Object> project = new LinkedHashMap<>();
        project.put("title", "JobPilot");
        project.put("organization", "Personal");
        project.put("startDate", null);
        project.put("endDate", null);
        project.put("description", "AI-powered job search orchestration platform.");
        project.put("extractedSkills", List.of("java", "spring-boot", "postgresql"));
        project.put("rawSourceExcerpt", "Personal project: JobPilot, an AI-powered job search platform");

        return Map.of(
                "experiences", List.of(experience),
                "educations", List.of(education),
                "projects", List.of(project),
                "certifications", List.of(),
                "achievements", List.of());
    }
}

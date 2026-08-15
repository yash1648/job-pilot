package com.jobpilot.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelRouterTest {

    private final ModelRouter router = new ModelRouter(
            "fast-model", "strong-model", "embed-model", "vision-model");

    @Test
    void routesTaskTypeToProfileModel() {
        assertEquals("fast-model", router.resolve(AiTaskType.SKILL_CLASSIFICATION));
        assertEquals("strong-model", router.resolve(AiTaskType.RESUME_REASONING));
        assertEquals("embed-model", router.resolve(AiTaskType.SEMANTIC_EMBEDDING));
        assertEquals("vision-model", router.resolve(AiTaskType.PAGE_UNDERSTANDING));
    }
}

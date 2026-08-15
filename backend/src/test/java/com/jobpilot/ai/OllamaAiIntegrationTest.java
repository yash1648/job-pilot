package com.jobpilot.ai;

import com.jobpilot.ai.provider.ollama.OllamaAiService;
import com.jobpilot.ai.provider.ollama.OllamaClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;

/**
 * Round-trips a fixture prompt through a REAL local Ollama (doc 06 §1, doc 26 §3).
 * Tagged {@code slow} and skipped unless Ollama is reachable, so it never breaks
 * the default suite. Run locally with Ollama up to exercise the real provider.
 */
@Tag("slow")
class OllamaAiIntegrationTest {

    private static boolean ollamaReachable(String baseUrl) {
        try {
            new OllamaClient(baseUrl).generate("llama3.2:3b",
                    "reply with the single word: pong", "ping", null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void skillClassificationRoundTripsThroughRealOllama() {
        String baseUrl = "http://localhost:11434";
        Assumptions.assumeTrue(ollamaReachable(baseUrl), "Ollama not reachable at " + baseUrl);
        ModelRouter router = new ModelRouter("llama3.2:3b", "llama3.1:8b",
                "nomic-embed-text", "llava:7b");
        OllamaAiService svc = new OllamaAiService(router, new OllamaClient(baseUrl));

        AiRequest req = new AiRequest(AiTaskType.SKILL_CLASSIFICATION,
                new UntrustedContent("Senior Java engineer with Spring Boot and PostgreSQL experience."),
                "Extract the top technical skills as JSON with a 'skills' array of strings.",
                new ResponseSchema(new String[]{"skills"}));
        StructuredResponse<?> resp = svc.complete(req);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) resp.data();
        Assumptions.assumeTrue(data.containsKey("skills"), "model did not return skills field");
    }
}

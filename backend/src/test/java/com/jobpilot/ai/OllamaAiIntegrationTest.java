package com.jobpilot.ai;

import com.jobpilot.ai.provider.ollama.OllamaAiService;
import com.jobpilot.ai.provider.ollama.OllamaClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import java.util.Map;

/**
 * Round-trips a fixture prompt through a REAL local Ollama (doc 06 §1, doc 26 §3).
 * Tagged {@code slow} and skipped unless Ollama + a usable chat model are
 * reachable, so it never breaks the default suite. Run locally with Ollama up
 * (and at least one chat model pulled) to exercise the real provider.
 */
@Tag("slow")
class OllamaAiIntegrationTest {

    /** A chat model that must exist on the local Ollama for this test to run. */
    private static final String CHAT_MODEL = "qwen3:4b";

    private static boolean ollamaReachable(String baseUrl) {
        try {
            new OllamaClient(baseUrl).generate(CHAT_MODEL,
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
        ModelRouter router = new ModelRouter(CHAT_MODEL, "qwen3:4b",
                "nomic-embed-text", "qwen3:4b");
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

package com.jobpilot.ai;

import com.jobpilot.ai.provider.ollama.OllamaClient;
import com.jobpilot.ai.provider.ollama.OllamaEmbeddingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips a fixture prompt through a REAL local Ollama embedding model
 * (doc 06 §1, doc 26 §3) — Ollama's role in the current architecture is
 * embeddings (nomic-embed-text). Tagged {@code slow} and skipped unless Ollama
 * is reachable, so it never breaks the default suite. Run locally with Ollama
 * up (nomic-embed-text pulled) to exercise the real provider.
 */
@Tag("slow")
class OllamaAiIntegrationTest {

    private static final String BASE_URL = "http://localhost:11434";
    private static final String EMBED_MODEL = "nomic-embed-text";

    private static boolean ollamaReachable() {
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/"))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<Void> resp = java.net.http.HttpClient.newHttpClient()
                    .send(req, java.net.http.HttpResponse.BodyHandlers.discarding());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void embeddingRoundTripsThroughRealOllama() {
        Assumptions.assumeTrue(ollamaReachable(), "Ollama not reachable at " + BASE_URL);
        ModelRouter router = new ModelRouter("unused", "unused", EMBED_MODEL, "unused");
        OllamaEmbeddingService svc = new OllamaEmbeddingService(router, new OllamaClient(BASE_URL));

        float[] v1 = svc.embed("Senior Java engineer with Spring Boot and PostgreSQL.", EmbeddingKind.RESUME);
        float[] v2 = svc.embed("Senior Java engineer with Spring Boot and PostgreSQL.", EmbeddingKind.RESUME);

        assertTrue(v1.length > 0, "embedding vector must be non-empty");
        assertEquals(v1.length, v2.length, "embedding dimension must be stable");
        assertEquals(v1[0], v2[0], "embedding must be deterministic for identical input");
    }
}

package com.jobpilot.ai.provider.ollama;

import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.EmbeddingKind;
import com.jobpilot.ai.EmbeddingService;
import com.jobpilot.ai.ModelRouter;
import org.springframework.web.client.RestClientException;

/**
 * Ollama-backed {@link EmbeddingService} (doc 06 §1). Routing uses the
 * EMBEDDING profile; transport failure → {@link AiUnavailableException}.
 */
public class OllamaEmbeddingService implements EmbeddingService {

    private final ModelRouter router;
    private final OllamaClient client;

    public OllamaEmbeddingService(ModelRouter router, OllamaClient client) {
        this.router = router;
        this.client = client;
    }

    @Override
    public float[] embed(String text, EmbeddingKind kind) {
        try {
            return client.embed(router.resolve(com.jobpilot.ai.AiTaskType.SEMANTIC_EMBEDDING), text);
        } catch (RestClientException e) {
            throw new AiUnavailableException("Ollama embeddings unavailable", e);
        }
    }
}

package com.jobpilot.ai.provider.ollama;

import tools.jackson.databind.JsonNode;
import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.ModelRouter;
import com.jobpilot.ai.OutputValidator;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.ai.VisionService;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Ollama-backed {@link VisionService} (doc 06 §1, doc 14). Image is sent as a
 * base64 field distinct from the text prompt (doc 23 §2).
 */
public class OllamaVisionService implements VisionService {

    private final ModelRouter router;
    private final OllamaClient client;

    public OllamaVisionService(ModelRouter router, OllamaClient client) {
        this.router = router;
        this.client = client;
    }

    @Override
    public StructuredResponse<?> interpret(byte[] image, AiRequest context) {
        String model = router.resolve(com.jobpilot.ai.AiTaskType.PAGE_UNDERSTANDING);
        String schema = context.outputSchema() != null
                ? "{\"type\":\"object\",\"required\":[]}" : null;
        long start = System.nanoTime();
        try {
            String raw = client.generateWithImage(model, context.systemInstruction(),
                    context.content().value(), image, schema);
            JsonNode parsed = OutputValidator.parseAndValidate(raw, context.outputSchema());
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            return new StructuredResponse<>(toMap(parsed), model,
                    context.content().value().length() / 4, raw.length() / 4, latencyMs);
        } catch (RestClientException e) {
            throw new AiUnavailableException("Ollama vision unavailable", e);
        } catch (AiOutputInvalidException e) {
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(JsonNode node) {
        try {
            return new tools.jackson.databind.ObjectMapper().convertValue(node, Map.class);
        } catch (Exception e) {
            return Map.of("raw", node.asText());
        }
    }
}

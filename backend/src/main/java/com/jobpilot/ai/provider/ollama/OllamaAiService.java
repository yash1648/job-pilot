package com.jobpilot.ai.provider.ollama;

import tools.jackson.databind.JsonNode;
import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.ModelRouter;
import com.jobpilot.ai.OutputValidator;
import com.jobpilot.ai.StructuredResponse;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Ollama-backed {@link AiService} (doc 06 §1). System instruction and
 * untrusted content are passed as distinct Ollama fields (structural
 * separation, doc 23 §2). Output is schema-validated; on failure it is
 * retried once with an error-correction note, then surfaced as
 * {@link AiOutputInvalidException} (doc 06 §6).
 */
public class OllamaAiService implements AiService {

    private final ModelRouter router;
    private final OllamaClient client;

    public OllamaAiService(ModelRouter router, OllamaClient client) {
        this.router = router;
        this.client = client;
    }

    @Override
    public StructuredResponse<?> complete(AiRequest request) {
        String model = router.resolve(request.taskType());
        Object schema = request.outputSchema() != null ? toJsonSchema(request.outputSchema()) : null;
        long start = System.nanoTime();
        try {
            String raw = tryGenerate(model, request, schema, null);
            JsonNode parsed = OutputValidator.parseAndValidate(raw, request.outputSchema());
            return build(parsed, model, request, raw, start);
        } catch (AiOutputInvalidException first) {
            // bounded retry with error-correction note (doc 06 §6)
            try {
                String corrected = request.systemInstruction()
                        + "\nThe previous response was invalid. Return ONLY the required JSON fields.";
                String raw = tryGenerate(model, request, schema, corrected);
                JsonNode parsed = OutputValidator.parseAndValidate(raw, request.outputSchema());
                return build(parsed, model, request, raw, start);
            } catch (AiOutputInvalidException retry) {
                throw retry;
            }
        }
    }

    private String tryGenerate(String model, AiRequest request, Object schema, String overrideSystem) {
        try {
            return client.generate(model,
                    overrideSystem != null ? overrideSystem : request.systemInstruction(),
                    request.content().value(),
                    schema);
        } catch (RestClientException e) {
            throw new AiUnavailableException("Ollama unavailable for " + request.taskType(), e);
        }
    }

    private StructuredResponse<?> build(JsonNode parsed, String model, AiRequest request, String raw, long start) {
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        return new StructuredResponse<>(
                toMap(parsed),
                model,
                estimateTokens(request),
                estimateTokens(raw),
                latencyMs);
    }

    /** Build a valid JSON-Schema object for Ollama's structured output (doc 06 §6). */
    private static java.util.Map<String, Object> toJsonSchema(com.jobpilot.ai.ResponseSchema schema) {
        java.util.Map<String, Object> props = new java.util.LinkedHashMap<>();
        for (String field : schema.requiredFields()) {
            props.put(field, new java.util.LinkedHashMap<>()); // any type; presence enforced
        }
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("type", "object");
        out.put("properties", props);
        out.put("required", java.util.List.of(schema.requiredFields()));
        return out;
    }

    private static int estimateTokens(AiRequest request) {
        return (request.systemInstruction().length() + request.content().value().length()) / 4;
    }

    private static int estimateTokens(String text) {
        return text.length() / 4;
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

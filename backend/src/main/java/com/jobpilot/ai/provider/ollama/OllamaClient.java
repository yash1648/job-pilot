package com.jobpilot.ai.provider.ollama;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.web.client.RestClient;

import com.jobpilot.ai.AiUnavailableException;

import java.time.Duration;

/**
 * Thin HTTP wrapper around the Ollama REST API (doc 06 §1). Lives in
 * {@code ai.provider.ollama} so nothing outside {@code ai..} can depend on it
 * directly (ModuleBoundaryTest). Calls are bounded by a timeout; transport
 * failures surface as {@link com.jobpilot.ai.AiUnavailableException} upstream.
 */
public class OllamaClient {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient(String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /** Generate text (optionally structured via a JSON-Schema {@code format} object). */
    public String generate(String model, String system, String prompt, Object formatSchema) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        if (system != null) {
            body.put("system", system);
        }
        body.put("prompt", prompt);
        if (formatSchema != null) {
            body.put("format", formatSchema);
        }
        body.put("stream", false);
        body.put("options", java.util.Map.of("timeout", 120));

        String response = restClient.post()
                .uri("/api/generate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return responseText(response);
    }

    /** Generate with an attached image (base64) for vision tasks. */
    public String generateWithImage(String model, String system, String prompt, byte[] image, Object formatSchema) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        if (system != null) {
            body.put("system", system);
        }
        body.put("prompt", prompt);
        body.put("images", new String[]{java.util.Base64.getEncoder().encodeToString(image)});
        if (formatSchema != null) {
            body.put("format", formatSchema);
        }
        body.put("stream", false);
        body.put("options", java.util.Map.of("timeout", 120));

        String response = restClient.post()
                .uri("/api/generate")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        return responseText(response);
    }

    /** Produce an embedding vector. */
    public float[] embed(String model, String text) {
        java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("prompt", text);

        String response = restClient.post()
                .uri("/api/embeddings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        try {
            JsonNode node = mapper.readTree(response);
            JsonNode embedding = node.get("embedding");
            float[] out = new float[embedding.size()];
            for (int i = 0; i < out.length; i++) {
                out[i] = (float) embedding.get(i).asDouble();
            }
            return out;
        } catch (Exception e) {
            throw new com.jobpilot.ai.AiUnavailableException("invalid embedding response", e);
        }
    }

    private String responseText(String raw) {
        try {
            JsonNode node = mapper.readTree(raw);
            JsonNode resp = node.get("response");
            if (resp != null && !resp.asText().isBlank()) {
                return resp.asText();
            }
            // reasoning models (e.g. qwen3) emit the answer in "thinking" and leave
            // "response" empty — fall back so generation still works (doc 06 models are
            // non-reasoning, but this keeps the client robust to either shape).
            JsonNode thinking = node.get("thinking");
            if (thinking != null && !thinking.asText().isBlank()) {
                return thinking.asText();
            }
            return "";
        } catch (Exception e) {
            throw new AiUnavailableException("invalid generate response", e);
        }
    }
}

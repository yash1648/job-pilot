package com.jobpilot.ai.provider.openai;

import tools.jackson.databind.JsonNode;

import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.OutputValidator;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.ai.VisionService;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible {@link VisionService} (doc 06 §1, doc 14). The image is sent
 * as a base64 data-URL content part distinct from the text prompt (doc 23 §2).
 * Reuses the same endpoint/chat surface as {@link OpenAiCompatibleAiService}.
 */
public class OpenAiCompatibleVisionService implements VisionService {

    private final String model;
    private final RestClient restClient;

    public OpenAiCompatibleVisionService(String baseUrl, String apiKey, String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public StructuredResponse<?> interpret(byte[] image, AiRequest context) {
        String dataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(image);
        Map<String, Object> textPart = Map.of("type", "text", "text", context.content().value());
        Map<String, Object> imagePart = Map.of("type", "image_url",
                "image_url", Map.of("url", dataUri));
        Map<String, Object> userMsg = Map.of("role", "user", "content", List.of(imagePart, textPart));
        Map<String, Object> sysMsg = Map.of("role", "system", "content", context.systemInstruction());
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(sysMsg, userMsg));
        body.put("temperature", 0);
        body.put("stream", false);
        if (context.outputSchema() != null) {
            body.put("response_format", Map.of("type", "json_object"));
        }
        long start = System.nanoTime();
        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            OpenAiCompatibleAiService.Completion c = OpenAiCompatibleAiService.parseResponse(response);
            JsonNode parsed = OutputValidator.parseAndValidate(c.content(), context.outputSchema());
            long latencyMs = (System.nanoTime() - start) / 1_000_000;
            return new StructuredResponse<>(OpenAiCompatibleAiService.toMap(parsed), model,
                    c.promptTokens(), c.completionTokens(), latencyMs);
        } catch (RestClientException e) {
            throw new AiUnavailableException("OpenAI-compatible vision unavailable", e);
        }
    }
}

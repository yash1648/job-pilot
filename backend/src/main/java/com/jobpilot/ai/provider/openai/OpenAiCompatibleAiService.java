package com.jobpilot.ai.provider.openai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.jobpilot.ai.AiOutputInvalidException;
import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.OutputValidator;
import com.jobpilot.ai.StructuredResponse;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * OpenAI-compatible {@link AiService} (doc 06 §1) for any provider that exposes
 * the {@code /chat/completions} surface (e.g. a local LLM gateway). The trusted
 * system instruction and the untrusted payload are sent as distinct message
 * roles (doc 23 §2); output is requested as JSON and schema-validated, with a
 * bounded retry on invalid output (doc 06 §6). Embeddings are served
 * separately by the Ollama provider.
 */
public class OpenAiCompatibleAiService implements AiService {

    private final String model;
    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenAiCompatibleAiService(String baseUrl, String apiKey, String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    @Override
    public StructuredResponse<?> complete(AiRequest request) {
        long start = System.nanoTime();
        try {
            Completion c = tryGenerate(request, request.systemInstruction());
            JsonNode parsed = OutputValidator.parseAndValidate(c.content(), request.outputSchema());
            return build(parsed, c, start);
        } catch (AiOutputInvalidException first) {
            // bounded retry with error-correction note (doc 06 §6)
            try {
                String corrected = request.systemInstruction()
                        + "\nThe previous response was invalid. Return ONLY the required JSON fields.";
                Completion c = tryGenerate(request, corrected);
                JsonNode parsed = OutputValidator.parseAndValidate(c.content(), request.outputSchema());
                return build(parsed, c, start);
            } catch (AiOutputInvalidException retry) {
                throw retry;
            }
        }
    }

    private Completion tryGenerate(AiRequest request, String systemInstruction) {
        Map<String, Object> sysMsg = Map.of("role", "system", "content", systemInstruction);
        Map<String, Object> userMsg = Map.of("role", "user", "content", request.content().value());
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(sysMsg, userMsg));
        body.put("temperature", 0);
        body.put("stream", false);
        body.put("response_format", Map.of("type", "json_object"));
        try {
            String response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseResponse(response);
        } catch (RestClientException e) {
            throw new AiUnavailableException("OpenAI-compatible endpoint unavailable for " + request.taskType(), e);
        }
    }

    /** Parse a chat/completions response into content + token usage. */
    static Completion parseResponse(String raw) {
        try {
            JsonNode node = new ObjectMapper().readTree(raw);
            JsonNode choices = node.get("choices");
            String content = null;
            if (choices != null && choices.isArray() && choices.size() > 0) {
                content = choices.get(0).path("message").path("content").asText();
            }
            if (content == null || content.isBlank()) {
                throw new AiOutputInvalidException("empty completion from OpenAI-compatible endpoint");
            }
            content = stripFences(content);
            int promptTokens = 0;
            int completionTokens = 0;
            JsonNode usage = node.get("usage");
            if (usage != null) {
                promptTokens = usage.path("prompt_tokens").asInt(0);
                completionTokens = usage.path("completion_tokens").asInt(0);
            }
            return new Completion(content, promptTokens, completionTokens);
        } catch (AiOutputInvalidException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("invalid response from OpenAI-compatible endpoint", e);
        }
    }

    /** Strip a possible markdown code fence so the payload parses as JSON. */
    static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (firstNewline >= 0 && end > firstNewline) {
                return t.substring(firstNewline + 1, end).trim();
            }
        }
        return t;
    }

    private StructuredResponse<?> build(JsonNode parsed, Completion c, long start) {
        long latencyMs = (System.nanoTime() - start) / 1_000_000;
        return new StructuredResponse<>(toMap(parsed), model, c.promptTokens(), c.completionTokens(), latencyMs);
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> toMap(JsonNode node) {
        try {
            return new ObjectMapper().convertValue(node, Map.class);
        } catch (Exception e) {
            return Map.of("raw", node.asText());
        }
    }

    /** A single model completion: extracted content plus token usage. */
    record Completion(String content, int promptTokens, int completionTokens) {
    }
}

package com.jobpilot.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Minimal output validation (doc 06 §6): confirms each required field path in
 * the {@link ResponseSchema} is present in the parsed JSON. A full JSON-Schema
 * validator is intentionally out of scope for the Wave 1 skeleton — required
 * field presence is the enforced contract; the Policy Engine adds
 * evidence-level validation later (doc 23 §4).
 */
public final class OutputValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OutputValidator() {
    }

    /** Parse + validate; throws {@link AiOutputInvalidException} if a required field is missing. */
    public static JsonNode parseAndValidate(String rawJson, ResponseSchema schema) {
        JsonNode node;
        try {
            node = MAPPER.readTree(rawJson);
        } catch (Exception e) {
            throw new AiOutputInvalidException("response is not valid JSON");
        }
        if (schema != null && schema.requiredFields() != null) {
            for (String field : schema.requiredFields()) {
                if (node.path(field).isMissingNode()) {
                    throw new AiOutputInvalidException("missing required field: " + field);
                }
            }
        }
        return node;
    }
}

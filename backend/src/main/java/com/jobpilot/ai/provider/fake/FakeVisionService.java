package com.jobpilot.ai.provider.fake;

import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.ai.VisionService;

import java.util.Map;

/**
 * Deterministic stand-in for {@link VisionService} (doc 26 §3).
 */
public class FakeVisionService implements VisionService {

    @Override
    public StructuredResponse<?> interpret(byte[] image, AiRequest context) {
        return new StructuredResponse<>(
                Map.of("text", "fake-extracted-text", "confidence", 0.99),
                "fake", 1, 1, 0L);
    }
}

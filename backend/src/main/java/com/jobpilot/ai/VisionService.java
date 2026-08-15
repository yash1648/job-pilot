package com.jobpilot.ai;

import com.jobpilot.ai.StructuredResponse;

/** Image/screenshot interpretation (doc 06 §1, doc 14). */
public interface VisionService {
    StructuredResponse<?> interpret(byte[] image, AiRequest context);
}

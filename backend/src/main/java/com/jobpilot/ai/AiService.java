package com.jobpilot.ai;

import com.jobpilot.ai.StructuredResponse;

/** Text/structured generation (doc 06 §1). */
public interface AiService {
    StructuredResponse<?> complete(AiRequest request);
}

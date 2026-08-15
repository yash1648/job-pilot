package com.jobpilot.ai;

/** Vector embedding (doc 06 §1). */
public interface EmbeddingService {
    float[] embed(String text, EmbeddingKind kind);
}

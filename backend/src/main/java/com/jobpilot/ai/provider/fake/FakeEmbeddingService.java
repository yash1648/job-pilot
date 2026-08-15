package com.jobpilot.ai.provider.fake;

import com.jobpilot.ai.EmbeddingKind;
import com.jobpilot.ai.EmbeddingService;

/**
 * Deterministic stand-in for {@link EmbeddingService} (doc 26 §3). Produces a
 * fixed-length vector derived from the text hash so callers get a stable,
 * non-zero embedding without a model. Dimension matches the documented
 * {@code candidate_embeddings} column (1536) for drop-in compatibility.
 */
public class FakeEmbeddingService implements EmbeddingService {

    private static final int DIM = 1536;

    @Override
    public float[] embed(String text, EmbeddingKind kind) {
        float[] vec = new float[DIM];
        int seed = text.hashCode();
        for (int i = 0; i < DIM; i++) {
            vec[i] = ((seed * (i + 31)) % 1000) / 1000.0f;
        }
        return vec;
    }
}

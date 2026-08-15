package com.jobpilot.ai;

/** Model profile a task is routed to (doc 06 §2). Concrete name resolved per environment. */
public enum ModelProfile {
    FAST,
    STRONG,
    EMBEDDING,
    VISION
}

package com.jobpilot.candidate.domain;

/** How aggressively JobPilot may apply on the candidate's behalf (doc 03 §JobPreference). */
public enum AutomationMode {
    FULLY_MANUAL,
    APPROVE_EVERY_APPLICATION,
    APPROVE_PER_COMPANY,
    APPROVE_PER_BATCH,
    AUTO_WITHIN_RULES
}

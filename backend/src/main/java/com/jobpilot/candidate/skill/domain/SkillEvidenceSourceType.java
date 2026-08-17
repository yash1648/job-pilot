package com.jobpilot.candidate.skill.domain;

/**
 * Source of a {@link SkillEvidence} row (doc 03 §2, doc 04 §2.2). Values match
 * the {@code skill_evidence.source_type} CHECK constraint exactly; achievements
 * are intentionally excluded (doc 07 §4 lists only these four evidence sources).
 */
public enum SkillEvidenceSourceType {
    EXPERIENCE,
    PROJECT,
    CERTIFICATION,
    EDUCATION
}
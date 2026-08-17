package com.jobpilot.candidate.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Provenance for a {@link Skill}: which extracted entity it was drawn from and
 * the verbatim excerpt that grounds the claim (doc 07 §4, doc 23 §4). The
 * {@code source_type} CHECK constraint restricts values to the four evidence
 * sources (doc 04 §2.2).
 */
@Entity
@Table(name = "skill_evidence")
public class SkillEvidence {

    @Id
    private UUID id;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private SkillEvidenceSourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private UUID sourceId;

    @Column(nullable = false)
    private String excerpt;

    @Column(nullable = false)
    private Double confidence;

    protected SkillEvidence() {
    }

    public SkillEvidence(UUID skillId, SkillEvidenceSourceType sourceType, UUID sourceId,
                         String excerpt, Double confidence) {
        this.id = UUID.randomUUID();
        this.skillId = skillId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.excerpt = excerpt;
        this.confidence = confidence;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSkillId() {
        return skillId;
    }

    public SkillEvidenceSourceType getSourceType() {
        return sourceType;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public Double getConfidence() {
        return confidence;
    }
}
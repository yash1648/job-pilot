package com.jobpilot.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Type;
import java.time.Instant;
import java.util.UUID;

/**
 * Vector embedding of a candidate's synthesized profile (doc 03 §2, doc 04 §2.2).
 * One per {@link CandidateProfile}, co-located with source rows for transactional
 * consistency (ADR-004). Dimension is 768 to match the nomic-embed-text model.
 */
@Entity
@Table(name = "candidate_embeddings")
public class CandidateEmbedding {

    @Id
    private UUID id;

    @Column(name = "candidate_profile_id", nullable = false, unique = true)
    private UUID candidateProfileId;

    @Type(PgVectorType.class)
    @Column(columnDefinition = "vector(768)", nullable = false)
    private float[] vector;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CandidateEmbedding() {
    }

    public CandidateEmbedding(UUID candidateProfileId) {
        this.id = UUID.randomUUID();
        this.candidateProfileId = candidateProfileId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

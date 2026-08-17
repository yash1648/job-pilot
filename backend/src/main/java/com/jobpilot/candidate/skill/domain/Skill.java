package com.jobpilot.candidate.skill.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * A normalized skill attributed to a candidate (doc 03 §2, doc 04 §2.2). Always
 * resolvable to at least one {@link SkillEvidence} row before it is used in
 * high-confidence matching/generation (Zero-Fabrication, doc 07 §4, doc 23 §4).
 */
@Entity
@Table(name = "skills")
public class Skill {

    @Id
    private UUID id;

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Column(nullable = false)
    private String name;

    @Column(name = "normalized_name", nullable = false)
    private String normalizedName;

    private String category;

    private String proficiency;

    @Column(name = "years_experience")
    private Double yearsExperience;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Skill() {
    }

    public Skill(UUID candidateProfileId, String name, String normalizedName) {
        this.id = UUID.randomUUID();
        this.candidateProfileId = candidateProfileId;
        this.name = name;
        this.normalizedName = normalizedName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public String getName() {
        return name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProficiency() {
        return proficiency;
    }

    public void setProficiency(String proficiency) {
        this.proficiency = proficiency;
    }

    public Double getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Double yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
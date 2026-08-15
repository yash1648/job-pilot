package com.jobpilot.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Candidate profile — aggregate root for everything candidate-side (doc 03 §2).
 * One per User, linked by {@code user_id} (not a JPA relation to the {@code user}
 * module, to keep module boundaries clean — doc 34 §3).
 */
@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    private String headline;

    @Column(name = "seniority_estimate")
    private String seniorityEstimate;

    @Column(name = "domain_classification")
    private String[] domainClassification;

    @Column(name = "career_trajectory_summary")
    private String careerTrajectorySummary;

    private String[] strengths;
    private String[] weaknesses;
    private String[] skillGaps;
    private String[] transferableSkills;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CandidateProfile() {
    }

    public CandidateProfile(UUID userId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.domainClassification = new String[0];
        this.strengths = new String[0];
        this.weaknesses = new String[0];
        this.skillGaps = new String[0];
        this.transferableSkills = new String[0];
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public String getSeniorityEstimate() {
        return seniorityEstimate;
    }

    public void setSeniorityEstimate(String seniorityEstimate) {
        this.seniorityEstimate = seniorityEstimate;
    }

    public String[] getDomainClassification() {
        return domainClassification;
    }

    public void setDomainClassification(String[] domainClassification) {
        this.domainClassification = domainClassification;
    }

    public String getCareerTrajectorySummary() {
        return careerTrajectorySummary;
    }

    public void setCareerTrajectorySummary(String careerTrajectorySummary) {
        this.careerTrajectorySummary = careerTrajectorySummary;
    }

    public String[] getStrengths() {
        return strengths;
    }

    public void setStrengths(String[] strengths) {
        this.strengths = strengths;
    }

    public String[] getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String[] weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String[] getSkillGaps() {
        return skillGaps;
    }

    public void setSkillGaps(String[] skillGaps) {
        this.skillGaps = skillGaps;
    }

    public String[] getTransferableSkills() {
        return transferableSkills;
    }

    public void setTransferableSkills(String[] transferableSkills) {
        this.transferableSkills = transferableSkills;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

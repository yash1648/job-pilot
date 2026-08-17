package com.jobpilot.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Shared shape of the candidate evidence entities (Experience / Education /
 * Project / Certification / Achievement, doc 03 §2). Every row is scoped to a
 * candidate profile by {@code candidateProfileId} and carries the verbatim
 * {@code rawSourceExcerpt} the row was extracted from — the Zero-Fabrication
 * grounding requirement (doc 07:21-23, doc 23 §4).
 */
@MappedSuperclass
public abstract class CandidateEvidence {

    @Id
    private UUID id;

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String organization;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String description;

    @Column(name = "extracted_skills")
    private String[] extractedSkills;

    @Column(name = "raw_source_excerpt")
    private String rawSourceExcerpt;

    protected CandidateEvidence() {
    }

    protected CandidateEvidence(UUID candidateProfileId, String title, String organization,
                                LocalDate startDate, LocalDate endDate, String description,
                                String[] extractedSkills, String rawSourceExcerpt) {
        this.id = UUID.randomUUID();
        this.candidateProfileId = candidateProfileId;
        this.title = title;
        this.organization = organization;
        this.startDate = startDate;
        this.endDate = endDate;
        this.description = description;
        this.extractedSkills = extractedSkills == null ? new String[0] : extractedSkills;
        this.rawSourceExcerpt = rawSourceExcerpt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public String getTitle() {
        return title;
    }

    public String getOrganization() {
        return organization;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getDescription() {
        return description;
    }

    public String[] getExtractedSkills() {
        return extractedSkills;
    }

    public String getRawSourceExcerpt() {
        return rawSourceExcerpt;
    }
}
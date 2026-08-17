package com.jobpilot.candidate.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * An achievement entry extracted from a resume (doc 03 §2). The domain model
 * defines achievements alongside the other evidence entities; no dedicated
 * {@code candidate.achievement} package exists in doc 34 §1, so this entity
 * lives in {@code candidate.domain} with the other shared candidate types.
 */
@Entity
@Table(name = "achievements")
public class Achievement extends CandidateEvidence {

    protected Achievement() {
    }

    public Achievement(UUID candidateProfileId, String title, String organization,
                       LocalDate startDate, LocalDate endDate, String description,
                       String[] extractedSkills, String rawSourceExcerpt) {
        super(candidateProfileId, title, organization, startDate, endDate,
                description, extractedSkills, rawSourceExcerpt);
    }
}
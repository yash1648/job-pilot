package com.jobpilot.candidate.education.domain;

import com.jobpilot.candidate.domain.CandidateEvidence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * An education entry extracted from a resume (doc 03 §2).
 */
@Entity
@Table(name = "educations")
public class Education extends CandidateEvidence {

    protected Education() {
    }

    public Education(UUID candidateProfileId, String title, String organization,
                     LocalDate startDate, LocalDate endDate, String description,
                     String[] extractedSkills, String rawSourceExcerpt) {
        super(candidateProfileId, title, organization, startDate, endDate,
                description, extractedSkills, rawSourceExcerpt);
    }
}
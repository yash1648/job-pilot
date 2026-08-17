package com.jobpilot.candidate.certification.domain;

import com.jobpilot.candidate.domain.CandidateEvidence;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * A certification entry extracted from a resume (doc 03 §2).
 */
@Entity
@Table(name = "certifications")
public class Certification extends CandidateEvidence {

    protected Certification() {
    }

    public Certification(UUID candidateProfileId, String title, String organization,
                         LocalDate startDate, LocalDate endDate, String description,
                         String[] extractedSkills, String rawSourceExcerpt) {
        super(candidateProfileId, title, organization, startDate, endDate,
                description, extractedSkills, rawSourceExcerpt);
    }
}
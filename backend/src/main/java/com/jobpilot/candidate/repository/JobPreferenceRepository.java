package com.jobpilot.candidate.repository;

import com.jobpilot.candidate.domain.JobPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobPreferenceRepository extends JpaRepository<JobPreference, UUID> {

    /** Scoping by owner profile (doc 22 §2). */
    java.util.Optional<JobPreference> findByCandidateProfileId(UUID candidateProfileId);
}

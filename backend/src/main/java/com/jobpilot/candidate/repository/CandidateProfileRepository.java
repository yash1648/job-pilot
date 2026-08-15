package com.jobpilot.candidate.repository;

import com.jobpilot.candidate.domain.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    /** Scoping by owner: a user can only ever resolve their own profile (doc 22 §2). */
    java.util.Optional<CandidateProfile> findByUserId(UUID userId);
}

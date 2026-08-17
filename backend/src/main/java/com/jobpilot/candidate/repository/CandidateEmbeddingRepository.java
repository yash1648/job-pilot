package com.jobpilot.candidate.repository;

import com.jobpilot.candidate.domain.CandidateEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence for {@link CandidateEmbedding} (doc 04 §2.2). One embedding per
 * candidate profile (unique constraint on candidate_profile_id).
 */
public interface CandidateEmbeddingRepository extends JpaRepository<CandidateEmbedding, UUID> {

    Optional<CandidateEmbedding> findByCandidateProfileId(UUID candidateProfileId);

    void deleteByCandidateProfileId(UUID candidateProfileId);
}

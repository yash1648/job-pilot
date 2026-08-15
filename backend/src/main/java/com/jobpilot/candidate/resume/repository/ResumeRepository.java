package com.jobpilot.candidate.resume.repository;

import com.jobpilot.candidate.resume.domain.Resume;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    /** Scoping by owner: only the caller's own resumes (doc 22 §2). */
    List<Resume> findByCandidateProfileId(UUID candidateProfileId);

    /** Scoped lookup — returns empty for a resume owned by another candidate. */
    Optional<Resume> findByIdAndCandidateProfileId(UUID id, UUID candidateProfileId);
}

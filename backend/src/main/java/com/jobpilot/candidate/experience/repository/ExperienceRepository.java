package com.jobpilot.candidate.experience.repository;

import com.jobpilot.candidate.experience.domain.Experience;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExperienceRepository extends JpaRepository<Experience, UUID> {

    List<Experience> findByCandidateProfileId(UUID candidateProfileId);
}
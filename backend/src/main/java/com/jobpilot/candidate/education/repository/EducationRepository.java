package com.jobpilot.candidate.education.repository;

import com.jobpilot.candidate.education.domain.Education;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EducationRepository extends JpaRepository<Education, UUID> {

    List<Education> findByCandidateProfileId(UUID candidateProfileId);
}
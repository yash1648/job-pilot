package com.jobpilot.candidate.repository;

import com.jobpilot.candidate.domain.Achievement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    List<Achievement> findByCandidateProfileId(UUID candidateProfileId);
}
package com.jobpilot.candidate.skill.repository;

import com.jobpilot.candidate.skill.domain.Skill;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    List<Skill> findByCandidateProfileId(UUID candidateProfileId);

    List<Skill> findByCandidateProfileIdAndNormalizedName(UUID candidateProfileId, String normalizedName);
}
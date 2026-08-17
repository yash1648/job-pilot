package com.jobpilot.candidate.skill.repository;

import com.jobpilot.candidate.skill.domain.SkillEvidence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, UUID> {

    List<SkillEvidence> findBySkillId(UUID skillId);
}
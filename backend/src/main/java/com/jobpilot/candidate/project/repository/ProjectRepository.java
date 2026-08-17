package com.jobpilot.candidate.project.repository;

import com.jobpilot.candidate.project.domain.Project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findByCandidateProfileId(UUID candidateProfileId);
}
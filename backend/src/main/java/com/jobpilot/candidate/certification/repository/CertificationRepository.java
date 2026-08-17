package com.jobpilot.candidate.certification.repository;

import com.jobpilot.candidate.certification.domain.Certification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    List<Certification> findByCandidateProfileId(UUID candidateProfileId);
}
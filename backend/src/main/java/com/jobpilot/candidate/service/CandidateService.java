package com.jobpilot.candidate.service;

import com.jobpilot.candidate.api.CandidateDtos.ProfilePatchRequest;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Candidate profile operations (doc 05 §2). Scoped by owner userId — the
 * repository resolves only the caller's own profile, so cross-candidate data
 * cannot leak even if a controller check is missed (doc 22 §2, doc 25 §5).
 */
@Service
public class CandidateService {

    private final CandidateProfileRepository repository;

    public CandidateService(CandidateProfileRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public CandidateProfile getProfile(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "candidate profile not found"));
    }

    @Transactional
    public CandidateProfile updateProfile(UUID userId, ProfilePatchRequest patch) {
        CandidateProfile profile = repository.findByUserId(userId)
                .orElseGet(() -> new CandidateProfile(userId));
        if (patch.headline() != null) {
            profile.setHeadline(patch.headline());
        }
        if (patch.seniorityEstimate() != null) {
            profile.setSeniorityEstimate(patch.seniorityEstimate());
        }
        if (patch.domainClassification() != null) {
            profile.setDomainClassification(patch.domainClassification());
        }
        if (patch.careerTrajectorySummary() != null) {
            profile.setCareerTrajectorySummary(patch.careerTrajectorySummary());
        }
        if (patch.strengths() != null) {
            profile.setStrengths(patch.strengths());
        }
        if (patch.weaknesses() != null) {
            profile.setWeaknesses(patch.weaknesses());
        }
        if (patch.skillGaps() != null) {
            profile.setSkillGaps(patch.skillGaps());
        }
        if (patch.transferableSkills() != null) {
            profile.setTransferableSkills(patch.transferableSkills());
        }
        profile.setUpdatedAt(Instant.now());
        return repository.save(profile);
    }
}

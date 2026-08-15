package com.jobpilot.candidate.service;

import com.jobpilot.candidate.api.CandidateDtos.PreferencesRequest;
import com.jobpilot.candidate.domain.JobPreference;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.repository.JobPreferenceRepository;
import com.jobpilot.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Job preferences operations (doc 05 §3). Scoped through the owner's
 * CandidateProfile (doc 22 §2). A profile must exist before preferences can
 * be read or replaced.
 */
@Service
public class PreferencesService {

    private final CandidateProfileRepository profileRepository;
    private final JobPreferenceRepository preferenceRepository;

    public PreferencesService(CandidateProfileRepository profileRepository,
                              JobPreferenceRepository preferenceRepository) {
        this.profileRepository = profileRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Transactional(readOnly = true)
    public JobPreference getPreferences(UUID userId) {
        UUID profileId = requireProfile(userId).getId();
        return preferenceRepository.findByCandidateProfileId(profileId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "preferences not found"));
    }

    @Transactional
    public JobPreference replacePreferences(UUID userId, PreferencesRequest request) {
        UUID profileId = requireProfile(userId).getId();
        JobPreference pref = preferenceRepository.findByCandidateProfileId(profileId)
                .orElseGet(() -> new JobPreference(profileId));
        pref.setTargetRoles(orEmpty(request.targetRoles()));
        pref.setExcludedRoles(orEmpty(request.excludedRoles()));
        pref.setLocations(orEmpty(request.locations()));
        pref.setWorkMode(request.workMode());
        pref.setMinSalary(request.minSalary());
        pref.setPreferredSalary(request.preferredSalary());
        pref.setEmploymentType(orEmpty(request.employmentType()));
        pref.setExperienceLevel(request.experienceLevel());
        pref.setCompanyAllowList(orEmpty(request.companyAllowList()));
        pref.setCompanyDenyList(orEmpty(request.companyDenyList()));
        pref.setTechnologies(orEmpty(request.technologies()));
        pref.setIndustries(orEmpty(request.industries()));
        pref.setRelocationWilling(request.relocationWilling() != null && request.relocationWilling());
        pref.setWorkAuthorization(request.workAuthorization());
        pref.setApplicationFrequency(request.applicationFrequency());
        pref.setAutomationMode(request.automationMode());
        pref.setUpdatedAt(Instant.now());
        return preferenceRepository.save(pref);
    }

    private com.jobpilot.candidate.domain.CandidateProfile requireProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "candidate profile not found"));
    }

    private static String[] orEmpty(String[] value) {
        return value != null ? value : new String[0];
    }
}

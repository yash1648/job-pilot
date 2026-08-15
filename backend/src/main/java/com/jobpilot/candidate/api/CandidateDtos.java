package com.jobpilot.candidate.api;

import com.jobpilot.candidate.domain.AutomationMode;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.domain.JobPreference;
import com.jobpilot.candidate.domain.WorkMode;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request/response DTOs for candidate profile + preferences endpoints (doc 05 §2/§3).
 */
public final class CandidateDtos {

    private CandidateDtos() {
    }

    /** PATCH body — every field optional; only provided fields are updated. */
    public record ProfilePatchRequest(
            String headline,
            String seniorityEstimate,
            String[] domainClassification,
            String careerTrajectorySummary,
            String[] strengths,
            String[] weaknesses,
            String[] skillGaps,
            String[] transferableSkills) {
    }

    public record ProfileResponse(
            String id,
            String userId,
            String headline,
            String seniorityEstimate,
            String[] domainClassification,
            String careerTrajectorySummary,
            String[] strengths,
            String[] weaknesses,
            String[] skillGaps,
            String[] transferableSkills) {

        public static ProfileResponse from(CandidateProfile p) {
            return new ProfileResponse(
                    p.getId().toString(),
                    p.getUserId().toString(),
                    p.getHeadline(),
                    p.getSeniorityEstimate(),
                    p.getDomainClassification(),
                    p.getCareerTrajectorySummary(),
                    p.getStrengths(),
                    p.getWeaknesses(),
                    p.getSkillGaps(),
                    p.getTransferableSkills());
        }
    }

    /** PUT body — full replace. Enums required (doc 03 enum sets). */
    public record PreferencesRequest(
            String[] targetRoles,
            String[] excludedRoles,
            String[] locations,
            @NotNull WorkMode workMode,
            BigDecimal minSalary,
            BigDecimal preferredSalary,
            String[] employmentType,
            String experienceLevel,
            String[] companyAllowList,
            String[] companyDenyList,
            String[] technologies,
            String[] industries,
            Boolean relocationWilling,
            String workAuthorization,
            String applicationFrequency,
            @NotNull AutomationMode automationMode) {
    }

    public record PreferencesResponse(
            String id,
            String candidateProfileId,
            String[] targetRoles,
            String[] excludedRoles,
            String[] locations,
            WorkMode workMode,
            BigDecimal minSalary,
            BigDecimal preferredSalary,
            String[] employmentType,
            String experienceLevel,
            String[] companyAllowList,
            String[] companyDenyList,
            String[] technologies,
            String[] industries,
            boolean relocationWilling,
            String workAuthorization,
            String applicationFrequency,
            AutomationMode automationMode) {

        public static PreferencesResponse from(JobPreference p) {
            return new PreferencesResponse(
                    p.getId().toString(),
                    p.getCandidateProfileId().toString(),
                    p.getTargetRoles(),
                    p.getExcludedRoles(),
                    p.getLocations(),
                    p.getWorkMode(),
                    p.getMinSalary(),
                    p.getPreferredSalary(),
                    p.getEmploymentType(),
                    p.getExperienceLevel(),
                    p.getCompanyAllowList(),
                    p.getCompanyDenyList(),
                    p.getTechnologies(),
                    p.getIndustries(),
                    p.isRelocationWilling(),
                    p.getWorkAuthorization(),
                    p.getApplicationFrequency(),
                    p.getAutomationMode());
        }
    }
}

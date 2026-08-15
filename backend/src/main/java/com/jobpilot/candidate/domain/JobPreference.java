package com.jobpilot.candidate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Job search preferences (doc 03 §JobPreference, doc 04 job_preferences).
 * Belongs to a CandidateProfile; scoped through it (doc 22 §2).
 */
@Entity
@Table(name = "job_preferences")
public class JobPreference {

    @Id
    private UUID id;

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Column(name = "target_roles")
    private String[] targetRoles;

    @Column(name = "excluded_roles")
    private String[] excludedRoles;

    private String[] locations;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false)
    private WorkMode workMode = WorkMode.ANY;

    @Column(name = "min_salary")
    private BigDecimal minSalary;

    @Column(name = "preferred_salary")
    private BigDecimal preferredSalary;

    @Column(name = "employment_type")
    private String[] employmentType;

    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(name = "company_allow_list")
    private String[] companyAllowList;

    @Column(name = "company_deny_list")
    private String[] companyDenyList;

    private String[] technologies;
    private String[] industries;

    @Column(name = "relocation_willing")
    private boolean relocationWilling;

    @Column(name = "work_authorization")
    private String workAuthorization;

    @Column(name = "application_frequency")
    private String applicationFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "automation_mode", nullable = false)
    private AutomationMode automationMode = AutomationMode.FULLY_MANUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobPreference() {
    }

    public JobPreference(UUID candidateProfileId) {
        this.id = UUID.randomUUID();
        this.candidateProfileId = candidateProfileId;
        this.targetRoles = new String[0];
        this.excludedRoles = new String[0];
        this.locations = new String[0];
        this.employmentType = new String[0];
        this.companyAllowList = new String[0];
        this.companyDenyList = new String[0];
        this.technologies = new String[0];
        this.industries = new String[0];
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public String[] getTargetRoles() {
        return targetRoles;
    }

    public void setTargetRoles(String[] targetRoles) {
        this.targetRoles = targetRoles;
    }

    public String[] getExcludedRoles() {
        return excludedRoles;
    }

    public void setExcludedRoles(String[] excludedRoles) {
        this.excludedRoles = excludedRoles;
    }

    public String[] getLocations() {
        return locations;
    }

    public void setLocations(String[] locations) {
        this.locations = locations;
    }

    public WorkMode getWorkMode() {
        return workMode;
    }

    public void setWorkMode(WorkMode workMode) {
        this.workMode = workMode;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getPreferredSalary() {
        return preferredSalary;
    }

    public void setPreferredSalary(BigDecimal preferredSalary) {
        this.preferredSalary = preferredSalary;
    }

    public String[] getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String[] employmentType) {
        this.employmentType = employmentType;
    }

    public String getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(String experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    public String[] getCompanyAllowList() {
        return companyAllowList;
    }

    public void setCompanyAllowList(String[] companyAllowList) {
        this.companyAllowList = companyAllowList;
    }

    public String[] getCompanyDenyList() {
        return companyDenyList;
    }

    public void setCompanyDenyList(String[] companyDenyList) {
        this.companyDenyList = companyDenyList;
    }

    public String[] getTechnologies() {
        return technologies;
    }

    public void setTechnologies(String[] technologies) {
        this.technologies = technologies;
    }

    public String[] getIndustries() {
        return industries;
    }

    public void setIndustries(String[] industries) {
        this.industries = industries;
    }

    public boolean isRelocationWilling() {
        return relocationWilling;
    }

    public void setRelocationWilling(boolean relocationWilling) {
        this.relocationWilling = relocationWilling;
    }

    public String getWorkAuthorization() {
        return workAuthorization;
    }

    public void setWorkAuthorization(String workAuthorization) {
        this.workAuthorization = workAuthorization;
    }

    public String getApplicationFrequency() {
        return applicationFrequency;
    }

    public void setApplicationFrequency(String applicationFrequency) {
        this.applicationFrequency = applicationFrequency;
    }

    public AutomationMode getAutomationMode() {
        return automationMode;
    }

    public void setAutomationMode(AutomationMode automationMode) {
        this.automationMode = automationMode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

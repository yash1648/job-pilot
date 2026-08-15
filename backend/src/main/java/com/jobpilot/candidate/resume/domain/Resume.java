package com.jobpilot.candidate.resume.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * An uploaded resume artifact (doc 03 §2, doc 07 §2). Immutable after upload —
 * the original bytes live in storage (never mutated); parsing produces separate
 * ResumeVersion snapshots (doc 07:33, later task). Scoped to a candidate
 * profile by {@code candidateProfileId}, not a JPA relation to keep module
 * boundaries clean (doc 34 §3).
 */
@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    private UUID id;

    @Column(name = "candidate_profile_id", nullable = false)
    private UUID candidateProfileId;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "storage_ref", nullable = false)
    private String storageRef;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "is_master", nullable = false)
    private boolean isMaster;

    @Enumerated(EnumType.STRING)
    @Column(name = "parse_status", nullable = false)
    private ResumeParseStatus parseStatus;

    @Column(name = "parse_failure_reason")
    private String parseFailureReason;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private Instant uploadedAt;

    protected Resume() {
    }

    public Resume(UUID candidateProfileId, String originalFilename, String storageRef, String mimeType) {
        this.id = UUID.randomUUID();
        this.candidateProfileId = candidateProfileId;
        this.originalFilename = originalFilename;
        this.storageRef = storageRef;
        this.mimeType = mimeType;
        this.isMaster = false;
        this.parseStatus = ResumeParseStatus.PENDING;
        this.uploadedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getCandidateProfileId() {
        return candidateProfileId;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getStorageRef() {
        return storageRef;
    }

    public String getMimeType() {
        return mimeType;
    }

    public boolean isMaster() {
        return isMaster;
    }

    public void setMaster(boolean master) {
        isMaster = master;
    }

    public ResumeParseStatus getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(ResumeParseStatus parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseFailureReason() {
        return parseFailureReason;
    }

    public void setParseFailureReason(String parseFailureReason) {
        this.parseFailureReason = parseFailureReason;
    }

    public Instant getUploadedAt() {
        return uploadedAt;
    }
}

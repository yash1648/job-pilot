package com.jobpilot.candidate.resume.api;

import com.jobpilot.candidate.resume.domain.Resume;

/**
 * Response DTOs for resume endpoints (doc 05 §2). The {@code storageRef} is an
 * internal opaque key and is intentionally NOT exposed to clients (doc 22 §4).
 */
public final class ResumeDtos {

    private ResumeDtos() {
    }

    public record ResumeResponse(
            String id,
            String candidateProfileId,
            String originalFilename,
            String mimeType,
            boolean isMaster,
            String parseStatus,
            String uploadedAt) {

        public static ResumeResponse from(Resume r) {
            return new ResumeResponse(
                    r.getId().toString(),
                    r.getCandidateProfileId().toString(),
                    r.getOriginalFilename(),
                    r.getMimeType(),
                    r.isMaster(),
                    r.getParseStatus().name(),
                    r.getUploadedAt().toString());
        }
    }
}

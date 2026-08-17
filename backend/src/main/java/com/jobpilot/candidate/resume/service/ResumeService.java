package com.jobpilot.candidate.resume.service;

import com.jobpilot.audit.api.AuditDtos.AuditEventRequest;
import com.jobpilot.audit.api.AuditService;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.resume.api.ResumeDtos.ResumeResponse;
import com.jobpilot.candidate.resume.domain.Resume;
import com.jobpilot.candidate.resume.domain.ResumeParseStatus;
import com.jobpilot.candidate.resume.repository.ResumeRepository;
import com.jobpilot.candidate.service.ProfileSynthesisService;
import com.jobpilot.candidate.skill.service.SkillExtractionService;
import com.jobpilot.common.exception.ApiException;
import com.jobpilot.storage.api.StorageDtos.StoredFile;
import com.jobpilot.storage.api.StorageDtos.StoreRequest;
import com.jobpilot.storage.api.StorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Resume upload + lifecycle (doc 05 §2, doc 07 §2). Stores the original via
 * {@link StorageService} (encrypted, content-sniffed, doc 22 §4/§5) and records
 * a PENDING parse status; actual parsing is a later stage (doc 07). All reads
 * are scoped to the caller's candidate profile (doc 22 §2, doc 25 §5).
 */
@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final StorageService storageService;
    private final ResumeParsingService parsingService;
    private final ResumeAiExtractionService extractionService;
    private final SkillExtractionService skillExtractionService;
    private final ProfileSynthesisService profileSynthesisService;
    private final AuditService auditService;

    private static final Logger log = LoggerFactory.getLogger(ResumeService.class);

    public ResumeService(ResumeRepository resumeRepository,
                          CandidateProfileRepository candidateProfileRepository,
                          StorageService storageService,
                          ResumeParsingService parsingService,
                           ResumeAiExtractionService extractionService,
                           SkillExtractionService skillExtractionService,
                           ProfileSynthesisService profileSynthesisService,
                           AuditService auditService) {
        this.resumeRepository = resumeRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.storageService = storageService;
        this.parsingService = parsingService;
        this.extractionService = extractionService;
        this.skillExtractionService = skillExtractionService;
        this.profileSynthesisService = profileSynthesisService;
        this.auditService = auditService;
    }

    @Transactional
    public ResumeResponse upload(UUID userId, MultipartFile file) {
        UUID candidateProfileId = candidateProfileId(userId);
        StoredFile stored;
        try {
            stored = storageService.store(new StoreRequest(
                    file.getBytes(), file.getOriginalFilename(),
                    file.getContentType(), userId, "resume"));
        } catch (IllegalArgumentException e) {
            // storage rejects bad MIME / oversize / empty (doc 22 §4) → 400
            throw new ApiException("INVALID_FILE", HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IOException e) {
            throw new ApiException("UPLOAD_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "could not read uploaded bytes");
        }
        Resume resume = new Resume(candidateProfileId, stored.originalFilename(),
                stored.storageRef(), stored.mimeType());
        return ResumeResponse.from(resumeRepository.save(resume));
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(UUID userId) {
        return resumeRepository.findByCandidateProfileId(candidateProfileId(userId)).stream()
                .map(ResumeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(UUID userId, UUID id) {
        return ResumeResponse.from(loadOwned(userId, id));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        Resume resume = loadOwned(userId, id);
        if (resume.isMaster()) {
            throw new ApiException("CONFLICT", HttpStatus.CONFLICT,
                    "cannot delete the master resume without designating a replacement");
        }
        storageService.delete(resume.getStorageRef());
        resumeRepository.delete(resume);
    }

    @Transactional
    public ResumeResponse setMaster(UUID userId, UUID id) {
        UUID candidateProfileId = candidateProfileId(userId);
        Resume target = resumeRepository.findByIdAndCandidateProfileId(id, candidateProfileId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "resume not found"));
        // demote all, promote this — partial unique index ux_resumes_one_master
        // guarantees exactly one master per candidate (doc 04 §3)
        resumeRepository.findByCandidateProfileId(candidateProfileId)
                .forEach(r -> r.setMaster(r.getId().equals(id)));
        return ResumeResponse.from(target);
    }

    /**
     * Parses a resume's stored bytes and records the outcome (doc 07 §2/§9):
     * text extraction succeeds AND AI evidence extraction produces schema-valid,
     * evidence-grounded entities → PARSED; a text-parse failure OR a
     * schema-invalid AI extraction → FAILED with a user-facing reason plus a
     * {@code ResumeParsingFailed} audit event (doc 07:102).
     */
    @Transactional
    public ResumeResponse parse(UUID userId, UUID resumeId) {
        Resume resume = loadOwned(userId, resumeId);
        byte[] bytes = storageService.retrieve(resume.getStorageRef());
        String parsedText;
        try {
            parsedText = parsingService.parse(bytes, resume.getMimeType()).text();
        } catch (ResumeParseException e) {
            return markFailed(resume, userId, e.reason());
        }
        try {
            extractionService.extract(resume.getCandidateProfileId(), parsedText);
            try {
                skillExtractionService.extractSkills(resume.getCandidateProfileId());
            } catch (Exception e) {
                return markFailed(resume, userId, "skill extraction failed: " + e.getMessage());
            }
            // Synthesis + embedding (doc 07 §2): best-effort. Extraction succeeded,
            // so a synthesis failure must NOT fail the parse — the profile stays
            // PARSED and the failure is logged + audited (doc 07 §9, doc 09).
            try {
                profileSynthesisService.synthesize(resume.getCandidateProfileId());
            } catch (Exception e) {
                log.warn("Profile synthesis failed for resume {} (candidate {}); resume remains PARSED. Reason: {}",
                        resumeId, resume.getCandidateProfileId(), e.getMessage());
                auditService.record(new AuditEventRequest(
                        "SYSTEM", userId.toString(),
                        "ProfileSynthesisFailed",
                        "Resume", resume.getId().toString(),
                        Map.of("reason", e.getMessage())));
            }
            resume.setParseStatus(ResumeParseStatus.PARSED);
            resume.setParseFailureReason(null);
            return ResumeResponse.from(resumeRepository.save(resume));
        } catch (ResumeExtractionException e) {
            return markFailed(resume, userId, e.reason());
        }
    }

    private ResumeResponse markFailed(Resume resume, UUID userId, String reason) {
        resume.setParseStatus(ResumeParseStatus.FAILED);
        resume.setParseFailureReason(reason);
        auditService.record(new AuditEventRequest(
                "SYSTEM",
                userId.toString(),
                "ResumeParsingFailed",
                "Resume",
                resume.getId().toString(),
                Map.of("reason", reason)));
        return ResumeResponse.from(resumeRepository.save(resume));
    }

    private UUID candidateProfileId(UUID userId) {
        return candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "candidate profile not found"))
                .getId();
    }

    private Resume loadOwned(UUID userId, UUID id) {
        return resumeRepository.findByIdAndCandidateProfileId(id, candidateProfileId(userId))
                .orElseThrow(() -> new ApiException("NOT_FOUND", HttpStatus.NOT_FOUND,
                        "resume not found"));
    }
}

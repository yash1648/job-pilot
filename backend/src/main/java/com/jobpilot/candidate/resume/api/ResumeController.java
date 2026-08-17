package com.jobpilot.candidate.resume.api;

import com.jobpilot.candidate.resume.service.ResumeService;
import com.jobpilot.candidate.resume.api.ResumeDtos.ResumeResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Resume endpoints (doc 05 §2). Upload returns 202 + PENDING (async parse is a
 * later stage, doc 07). All operations are scoped to the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/candidate/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    public ResponseEntity<ResumeResponse> upload(@RequestParam("resume") MultipartFile file,
                                                Principal principal) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(resumeService.upload(userId(principal), file));
    }

    @GetMapping
    public ResponseEntity<List<ResumeResponse>> list(Principal principal) {
        return ResponseEntity.ok(resumeService.list(userId(principal)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResumeResponse> get(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(resumeService.get(userId(principal), id));
    }

    /**
     * Triggers parse of a previously uploaded resume: text extraction → AI
     * evidence extraction → skill extraction → profile synthesis + embedding
     * (doc 07 §2). Returns the resulting resume status (PARSED or FAILED).
     * Scoped to the authenticated user.
     */
    @PostMapping("/{id}/parse")
    public ResponseEntity<ResumeResponse> parse(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(resumeService.parse(userId(principal), id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, Principal principal) {
        resumeService.delete(userId(principal), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/set-master")
    public ResponseEntity<ResumeResponse> setMaster(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(resumeService.setMaster(userId(principal), id));
    }

    private static UUID userId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}

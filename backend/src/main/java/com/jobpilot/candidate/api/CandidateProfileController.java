package com.jobpilot.candidate.api;

import com.jobpilot.candidate.service.CandidateService;
import com.jobpilot.candidate.api.CandidateDtos.ProfilePatchRequest;
import com.jobpilot.candidate.api.CandidateDtos.ProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Candidate profile endpoints (doc 05 §2). Scoped by the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/candidate/profile")
public class CandidateProfileController {

    private final CandidateService candidateService;

    public CandidateProfileController(CandidateService candidateService) {
        this.candidateService = candidateService;
    }

    @GetMapping
    public ResponseEntity<ProfileResponse> get(Principal principal) {
        return ResponseEntity.ok(ProfileResponse.from(candidateService.getProfile(userId(principal))));
    }

    @PatchMapping
    public ResponseEntity<ProfileResponse> patch(@Valid @RequestBody ProfilePatchRequest patch,
                                                Principal principal) {
        return ResponseEntity.ok(ProfileResponse.from(candidateService.updateProfile(userId(principal), patch)));
    }

    private static UUID userId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}

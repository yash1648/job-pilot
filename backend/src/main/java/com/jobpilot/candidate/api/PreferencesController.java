package com.jobpilot.candidate.api;

import com.jobpilot.candidate.service.PreferencesService;
import com.jobpilot.candidate.api.CandidateDtos.PreferencesRequest;
import com.jobpilot.candidate.api.CandidateDtos.PreferencesResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * Job preferences endpoints (doc 05 §3). Scoped by the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;

    public PreferencesController(PreferencesService preferencesService) {
        this.preferencesService = preferencesService;
    }

    @GetMapping
    public ResponseEntity<PreferencesResponse> get(Principal principal) {
        return ResponseEntity.ok(PreferencesResponse.from(preferencesService.getPreferences(userId(principal))));
    }

    @PutMapping
    public ResponseEntity<PreferencesResponse> put(@Valid @RequestBody PreferencesRequest request,
                                                  Principal principal) {
        return ResponseEntity.ok(PreferencesResponse.from(preferencesService.replacePreferences(userId(principal), request)));
    }

    private static UUID userId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}

package com.jobpilot.candidate.resume.service;

import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.resume.TestResumeFixtures;
import com.jobpilot.candidate.resume.api.ResumeDtos.ResumeResponse;
import com.jobpilot.candidate.resume.domain.Resume;
import com.jobpilot.candidate.resume.repository.ResumeRepository;
import com.jobpilot.storage.api.StorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * ResumeService.parse() status recording (doc 07 §9): a valid file → PARSED,
 * a corrupt file → FAILED with a user-facing reason. No Spring context — plain
 * Mockito with the real ResumeParsingService.
 */
@ExtendWith(MockitoExtension.class)
class ResumeServiceParseStatusTest {

    @Mock
    ResumeRepository resumeRepository;

    @Mock
    CandidateProfileRepository candidateProfileRepository;

    @Mock
    StorageService storageService;

    private ResumeService newService() {
        return new ResumeService(resumeRepository, candidateProfileRepository, storageService,
                new ResumeParsingService());
    }

    @Test
    void validResumeMarksParsed() throws Exception {
        UUID userId = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile(userId);
        Resume resume = new Resume(profile.getId(), "cv.pdf", "owner/ref", "application/pdf");
        stubOwnership(userId, profile, resume);
        when(storageService.retrieve(resume.getStorageRef()))
                .thenReturn(TestResumeFixtures.textPdf("Senior Java Engineer"));

        ResumeResponse resp = newService().parse(userId, resume.getId());

        assertEquals("PARSED", resp.parseStatus());
        assertNull(resume.getParseFailureReason());
    }

    @Test
    void corruptResumeMarksFailedWithReason() {
        UUID userId = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile(userId);
        Resume resume = new Resume(profile.getId(), "cv.pdf", "owner/ref", "application/pdf");
        stubOwnership(userId, profile, resume);
        when(storageService.retrieve(resume.getStorageRef()))
                .thenReturn(TestResumeFixtures.corruptPdf());

        ResumeResponse resp = newService().parse(userId, resume.getId());

        assertEquals("FAILED", resp.parseStatus());
        assertNotNull(resume.getParseFailureReason());
        assertTrue(resume.getParseFailureReason().contains("unreadable"),
                resume.getParseFailureReason());
    }

    private void stubOwnership(UUID userId, CandidateProfile profile, Resume resume) {
        when(candidateProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(resumeRepository.findByIdAndCandidateProfileId(resume.getId(), profile.getId()))
                .thenReturn(Optional.of(resume));
        when(resumeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }
}

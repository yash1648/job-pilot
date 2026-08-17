package com.jobpilot.candidate.service;

import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.AiUnavailableException;
import com.jobpilot.ai.EmbeddingService;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.candidate.domain.CandidateEmbedding;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.repository.CandidateEmbeddingRepository;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.repository.SkillRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProfileSynthesisService} (doc 07 §2, §5): happy-path
 * synthesis persists derived fields + a 768-dim {@link CandidateEmbedding};
 * AI-unavailable degrades to heuristic seniority and skips the embedding.
 */
@ExtendWith(MockitoExtension.class)
class ProfileSynthesisServiceTest {

    @Mock
    AiService aiService;
    @Mock
    EmbeddingService embeddingService;
    @Mock
    CandidateProfileRepository profileRepository;
    @Mock
    SkillRepository skillRepository;
    @Mock
    ExperienceRepository experienceRepository;
    @Mock
    CandidateEmbeddingRepository embeddingRepository;

    private ProfileSynthesisService service() {
        return new ProfileSynthesisService(aiService, embeddingService, profileRepository,
                skillRepository, experienceRepository, embeddingRepository);
    }

    private Experience mockExperience(String title, LocalDate start, LocalDate end) {
        Experience e = Mockito.mock(Experience.class);
        when(e.getTitle()).thenReturn(title);
        when(e.getStartDate()).thenReturn(start);
        when(e.getEndDate()).thenReturn(end);
        when(e.getOrganization()).thenReturn("Acme");
        when(e.getDescription()).thenReturn("did things");
        return e;
    }

    @Test
    void synthesizesDerivedFieldsAndPersists768DimEmbedding() {
        UUID cid = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile(UUID.randomUUID());
        when(profileRepository.findById(cid)).thenReturn(Optional.of(profile));
        Experience exp = mockExperience("Senior Backend Engineer",
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 1));
        when(experienceRepository.findByCandidateProfileId(cid)).thenReturn(List.of(exp));
        Skill skill = Mockito.mock(Skill.class);
        when(skill.getName()).thenReturn("java");
        when(skill.getCategory()).thenReturn("language");
        when(skillRepository.findByCandidateProfileId(cid)).thenReturn(List.of(skill));

        Map<String, Object> ai = Map.of(
                "domainClassification", List.of("backend", "fintech"),
                "seniorityEstimate", "SENIOR",
                "careerTrajectorySummary", "IC progressing to leadership",
                "strengths", List.of("java", "systems design"),
                "weaknesses", List.of("public speaking"),
                "skillGaps", List.of("kubernetes"),
                "transferableSkills", List.of("mentoring"));
        when(aiService.complete(any())).thenReturn((StructuredResponse) new StructuredResponse<>(ai, "fake", 1, 1, 1L));
        when(embeddingRepository.findByCandidateProfileId(cid)).thenReturn(Optional.empty());
        when(embeddingService.embed(any(), any())).thenReturn(new float[768]);

        service().synthesize(cid);

        assertEquals("SENIOR", profile.getSeniorityEstimate());
        assertEquals("backend", profile.getDomainClassification()[0]);
        assertEquals("IC progressing to leadership", profile.getCareerTrajectorySummary());
        assertEquals("java", profile.getStrengths()[0]);
        assertEquals("kubernetes", profile.getSkillGaps()[0]);

        ArgumentCaptor<CandidateEmbedding> captor = ArgumentCaptor.forClass(CandidateEmbedding.class);
        verify(embeddingRepository).save(captor.capture());
        CandidateEmbedding emb = captor.getValue();
        assertEquals(768, emb.getVector().length);
        assertEquals("nomic-embed-text", emb.getModelVersion());
        assertNotNull(emb.getUpdatedAt());
    }

    @Test
    void aiUnavailableFallsBackToHeuristicSeniorityAndSkipsEmbedding() {
        UUID cid = UUID.randomUUID();
        CandidateProfile profile = new CandidateProfile(UUID.randomUUID());
        when(profileRepository.findById(cid)).thenReturn(Optional.of(profile));
        Experience exp = mockExperience("Senior Backend Engineer",
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 1));
        when(experienceRepository.findByCandidateProfileId(cid)).thenReturn(List.of(exp));
        when(skillRepository.findByCandidateProfileId(cid)).thenReturn(List.of());
        when(aiService.complete(any())).thenThrow(new AiUnavailableException("model down", null));

        service().synthesize(cid);

        // heuristic from "Senior ..." + 4y → SENIOR
        assertEquals("SENIOR", profile.getSeniorityEstimate());
        assertNull(profile.getCareerTrajectorySummary());
        verify(embeddingRepository, never()).save(any());
    }
}

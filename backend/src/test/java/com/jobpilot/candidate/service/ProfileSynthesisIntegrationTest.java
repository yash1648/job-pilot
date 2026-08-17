package com.jobpilot.candidate.service;

import com.jobpilot.ai.AiRequest;
import com.jobpilot.ai.AiService;
import com.jobpilot.ai.StructuredResponse;
import com.jobpilot.candidate.domain.CandidateEmbedding;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.repository.CandidateEmbeddingRepository;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.repository.SkillRepository;
import com.jobpilot.user.domain.User;
import com.jobpilot.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link ProfileSynthesisService} (doc 07 §2, §5) against a
 * real Postgres (pgvector) with the fake provider. The {@link AiService} is
 * mocked to return synthesis-shaped output so derived fields are deterministic;
 * the real {@code FakeEmbeddingService} produces the 768-dim vector. Asserts the
 * derived fields and the persisted {@link CandidateEmbedding}.
 */
@SpringBootTest
@Testcontainers
class ProfileSynthesisIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("jobpilot")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("jobpilot.ai.provider", () -> "fake");
        registry.add("jobpilot.storage.root-dir", () -> System.getProperty("java.io.tmpdir") + "/jobpilot-syn-storage");
        registry.add("jobpilot.storage.encryption.enabled", () -> "false");
        registry.add("jobpilot.security.jwt.secret", () -> "test-secret-that-is-long-enough-for-hmac-256-bits");
    }

    @Autowired
    ProfileSynthesisService synthesisService;
    @Autowired
    CandidateProfileRepository profileRepository;
    @Autowired
    ExperienceRepository experienceRepository;
    @Autowired
    SkillRepository skillRepository;
    @Autowired
    CandidateEmbeddingRepository embeddingRepository;
    @Autowired
    UserRepository userRepository;

    @MockitoBean
    AiService aiService;

    @Test
    void synthesizesAndPersistsEmbedding() {
        UUID userId = UUID.randomUUID();
        User user = userRepository.save(new User(userId, "syn-" + userId + "@example.com", "hash"));
        CandidateProfile profile = profileRepository.save(new CandidateProfile(user.getId()));
        UUID cid = profile.getId();

        experienceRepository.save(new Experience(cid, "Senior Backend Engineer", "Acme",
                LocalDate.of(2020, 1, 1), LocalDate.of(2024, 1, 1),
                "Built Java microservices", new String[]{"java"}, "excerpt"));
        skillRepository.save(new Skill(cid, "java", "java"));

        when(aiService.complete(any())).thenReturn((StructuredResponse) new StructuredResponse<>(Map.of(
                "domainClassification", List.of("backend"),
                "seniorityEstimate", "SENIOR",
                "careerTrajectorySummary", "IC progressing to leadership",
                "strengths", List.of("java"),
                "weaknesses", List.of("public speaking"),
                "skillGaps", List.of("kubernetes"),
                "transferableSkills", List.of("mentoring")), "fake", 1, 1, 1L));

        synthesisService.synthesize(cid);

        CandidateProfile reloaded = profileRepository.findById(cid).orElseThrow();
        assertEquals("SENIOR", reloaded.getSeniorityEstimate());
        assertEquals("backend", reloaded.getDomainClassification()[0]);

        CandidateEmbedding emb = embeddingRepository.findByCandidateProfileId(cid).orElseThrow();
        assertEquals(768, emb.getVector().length);
        assertNotNull(emb.getModelVersion());
    }
}

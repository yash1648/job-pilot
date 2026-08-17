package com.jobpilot.candidate.resume.service;

import com.jobpilot.candidate.domain.CandidateEmbedding;
import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.certification.repository.CertificationRepository;
import com.jobpilot.candidate.education.repository.EducationRepository;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.project.repository.ProjectRepository;
import com.jobpilot.candidate.resume.TestResumeFixtures;
import com.jobpilot.candidate.resume.api.ResumeDtos.ResumeResponse;
import com.jobpilot.candidate.repository.CandidateEmbeddingRepository;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.domain.SkillEvidence;
import com.jobpilot.candidate.skill.repository.SkillEvidenceRepository;
import com.jobpilot.candidate.skill.repository.SkillRepository;
import com.jobpilot.candidate.skill.service.SkillExtractionService;
import com.jobpilot.user.domain.User;
import com.jobpilot.user.repository.UserRepository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end resume pipeline against a REAL local Ollama (doc 07 §2/§4, doc 26
 * §3): upload → text parse → AI evidence extraction → skill derivation. Boots
 * the full Spring context with {@code jobpilot.ai.provider=ollama} and a real
 * Postgres (Flyway applies the schema, including the {@code skill_evidence}
 * CHECK constraint). Tagged {@code slow} and self-skipping unless Ollama +
 * {@code qwen3:4b} are reachable, so it never breaks the default suite. Run
 * locally with Ollama up to exercise the real provider end-to-end.
 */
@Tag("slow")
@SpringBootTest
@Testcontainers
class ResumePipelineIntegrationTest {

    private static final String BASE_URL = "http://localhost:3001/v1";
    private static final String API_KEY = System.getenv("FREELLMAPI_API_KEY");
    private static final String CHAT_MODEL = "auto";

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
        registry.add("jobpilot.ai.provider", () -> "openai");
        registry.add("jobpilot.ai.openai.base-url", () -> BASE_URL);
        registry.add("jobpilot.ai.openai.api-key", () -> API_KEY);
        registry.add("jobpilot.ai.openai.model", () -> CHAT_MODEL);
        registry.add("jobpilot.ai.ollama.base-url", () -> "http://localhost:11434");
        registry.add("jobpilot.storage.root-dir", () -> System.getProperty("java.io.tmpdir") + "/jobpilot-e2e-storage");
        registry.add("jobpilot.storage.encryption.enabled", () -> "false");
        registry.add("jobpilot.security.jwt.secret", () -> "test-secret-that-is-long-enough-for-hmac-256-bits");
    }

    @Autowired
    ResumeService resumeService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CandidateProfileRepository candidateProfileRepository;

    @Autowired
    ExperienceRepository experienceRepository;

    @Autowired
    EducationRepository educationRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    CertificationRepository certificationRepository;

    @Autowired
    SkillRepository skillRepository;

    @Autowired
    SkillEvidenceRepository skillEvidenceRepository;

    @Autowired
    CandidateEmbeddingRepository embeddingRepository;

    @Test
    void realOllamaPipelineExtractsEntitiesAndSkills() throws Exception {
        Assumptions.assumeTrue(endpointReachable(), "OpenAI-compatible endpoint not reachable at " + BASE_URL);

        UUID userId = UUID.randomUUID();
        User user = userRepository.save(new User(userId, "e2e-" + userId + "@example.com", "hash"));
        CandidateProfile profile = candidateProfileRepository.save(new CandidateProfile(user.getId()));
        UUID cid = profile.getId();

        byte[] pdf = TestResumeFixtures.textPdf("""
                John Doe
                Senior Backend Engineer — Acme Corp (2021-present)
                Built Java microservices on Spring Boot and PostgreSQL. Used React and Node.js.
                Education: BSc Computer Science, State University (2015-2019)
                Projects: JobPilot — AI-powered job search platform (Java, Spring Boot, React).
                Certifications: AWS Certified Solutions Architect.
                """);

        ResumeResponse uploaded = resumeService.upload(userId,
                new MockMultipartFile("file", "cv.pdf", "application/pdf", pdf));
        ResumeResponse parsed = resumeService.parse(userId, UUID.fromString(uploaded.id()));

        if (!"PARSED".equals(parsed.parseStatus())) {
            System.out.println("PARSE FAILED REASON: " + parsed.parseFailureReason());
        }
        assertEquals("PARSED", parsed.parseStatus());

        // evidence entities persisted with non-empty excerpts (Zero-Fabrication, doc 23 §4)
        var experiences = experienceRepository.findByCandidateProfileId(cid);
        var educations = educationRepository.findByCandidateProfileId(cid);
        var projects = projectRepository.findByCandidateProfileId(cid);
        var certifications = certificationRepository.findByCandidateProfileId(cid);

        var allEvidence = new java.util.ArrayList<>();
        allEvidence.addAll(experiences);
        allEvidence.addAll(educations);
        allEvidence.addAll(projects);
        allEvidence.addAll(certifications);
        assertFalse(allEvidence.isEmpty(), "expected at least one extracted evidence entity");

        assertTrue(experiences.stream().allMatch(e -> nonBlank(e.getRawSourceExcerpt())),
                "every experience must carry a non-empty rawSourceExcerpt");
        assertTrue(educations.stream().allMatch(e -> nonBlank(e.getRawSourceExcerpt())),
                "every education must carry a non-empty rawSourceExcerpt");
        assertTrue(projects.stream().allMatch(p -> nonBlank(p.getRawSourceExcerpt())),
                "every project must carry a non-empty rawSourceExcerpt");
        assertTrue(certifications.stream().allMatch(c -> nonBlank(c.getRawSourceExcerpt())),
                "every certification must carry a non-empty rawSourceExcerpt");

        // skills + evidence derived from the extracted entities (doc 07 §4). A
        // skill evidenced by multiple sources gets one evidence row per source.
        List<Skill> skills = skillRepository.findByCandidateProfileId(cid);
        assertFalse(skills.isEmpty(), "expected derived skills from extracted evidence");
        for (Skill s : skills) {
            List<SkillEvidence> ev = skillEvidenceRepository.findBySkillId(s.getId());
            assertFalse(ev.isEmpty(), "every skill needs at least one evidence row");
            assertTrue(ev.stream().allMatch(e -> e.getConfidence() == 0.9),
                    "every evidence row must carry the demonstrated confidence");
            assertTrue(ev.stream().allMatch(e -> e.getSourceType() != null),
                    "every evidence row must reference a source type");
            assertFalse(s.getNormalizedName().isBlank(), "skill must be normalized");
        }

        // CandidateEmbedding persisted via nomic-embed-text (768-dim) (doc 07 §2)
        CandidateEmbedding embedding = embeddingRepository.findByCandidateProfileId(cid)
                .orElseThrow(() -> new AssertionError("expected a CandidateEmbedding after parse"));
        assertEquals(768, embedding.getVector().length, "embedding must be 768-dim (nomic-embed-text)");
        assertTrue(nonBlank(embedding.getModelVersion()), "embedding must record its model version");
    }

    private static boolean nonBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean endpointReachable() {
        try {
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(BASE_URL + "/models"))
                    .header("Authorization", "Bearer " + API_KEY)
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            java.net.http.HttpResponse<String> resp = java.net.http.HttpClient.newHttpClient()
                    .send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}

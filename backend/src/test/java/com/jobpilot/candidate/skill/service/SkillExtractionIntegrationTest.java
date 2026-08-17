package com.jobpilot.candidate.skill.service;

import com.jobpilot.candidate.domain.CandidateProfile;
import com.jobpilot.candidate.experience.domain.Experience;
import com.jobpilot.candidate.experience.repository.ExperienceRepository;
import com.jobpilot.candidate.project.domain.Project;
import com.jobpilot.candidate.project.repository.ProjectRepository;
import com.jobpilot.candidate.repository.CandidateProfileRepository;
import com.jobpilot.candidate.skill.domain.Skill;
import com.jobpilot.candidate.skill.domain.SkillEvidence;
import com.jobpilot.candidate.skill.domain.SkillEvidenceSourceType;
import com.jobpilot.candidate.skill.repository.SkillEvidenceRepository;
import com.jobpilot.candidate.skill.repository.SkillRepository;
import com.jobpilot.user.domain.User;
import com.jobpilot.user.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
 * End-to-end skill extraction against a real Postgres (Flyway migrations apply
 * the {@code skills}/{@code skill_evidence} schema, including the
 * {@code source_type} CHECK constraint — doc 04 §2.2). Boots the full context
 * so the entity mappings and the constraint are exercised for real.
 */
@SpringBootTest
@Testcontainers
class SkillExtractionIntegrationTest {

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
        registry.add("jobpilot.storage.root-dir", () -> System.getProperty("java.io.tmpdir") + "/jobpilot-it-storage");
        registry.add("jobpilot.storage.encryption.enabled", () -> "false");
        registry.add("jobpilot.security.jwt.secret", () -> "test-secret-that-is-long-enough-for-hmac-256-bits");
    }

    @Autowired
    CandidateProfileRepository candidateProfileRepository;

    @Autowired
    ExperienceRepository experienceRepository;

    @Autowired
    ProjectRepository projectRepository;

    @Autowired
    SkillRepository skillRepository;

    @Autowired
    SkillEvidenceRepository skillEvidenceRepository;

    @Autowired
    SkillExtractionService skillExtractionService;

    @Autowired
    UserRepository userRepository;

    @Test
    void extractsAndPersistsSkillsWithEvidence() {
        User user = userRepository.save(new User(UUID.randomUUID(), "skill-it@example.com", "hash"));
        CandidateProfile profile = candidateProfileRepository.save(new CandidateProfile(user.getId()));
        UUID cid = profile.getId();

        experienceRepository.save(new Experience(cid, "Eng", "Acme", null, null, "built X",
                new String[]{"ReactJS", "Node.js"}, "exp excerpt"));
        projectRepository.save(new Project(cid, "P", "Acme", null, null, "did Y",
                new String[]{"Java", "Spring Boot"}, "pr excerpt"));

        List<Skill> skills = skillExtractionService.extractSkills(cid);

        assertEquals(4, skills.size());
        // normalization applied: ReactJS -> react, Node.js -> node, Spring Boot -> spring boot
        List<String> norms = skills.stream().map(Skill::getNormalizedName).sorted().toList();
        assertEquals(List.of("java", "node", "react", "spring boot"), norms);

        List<SkillEvidence> allEvidence = skills.stream()
                .flatMap(s -> skillEvidenceRepository.findBySkillId(s.getId()).stream())
                .toList();
        assertEquals(4, allEvidence.size());
        assertTrue(allEvidence.stream().allMatch(e -> e.getConfidence() == 0.9));
        assertTrue(allEvidence.stream().allMatch(e ->
                e.getSourceType() == SkillEvidenceSourceType.EXPERIENCE
                        || e.getSourceType() == SkillEvidenceSourceType.PROJECT));
        // CHECK constraint holds: every persisted source_type is one of the four allowed
        assertFalse(allEvidence.isEmpty());
    }
}
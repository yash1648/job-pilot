package com.jobpilot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Database tests (doc 26 §1): apply all Flyway migrations against a fresh
 * Postgres and verify doc 04 constraint behavior — ux_resumes_one_master and
 * skill_evidence confidence CHECK (doc 04:94-95, 130).
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.data.redis.repositories.enabled=false"
})
class MigrationConstraintTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("jobpilot")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplied() {
        List<String> applied = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = true ORDER BY installed_rank", String.class);
        assertThat(applied).contains("1", "2");
    }

    @Test
    void secondMasterResumeViolatesUniquePartialIndex() {
        UUID candidate = newCandidate();

        jdbc.update("""
                INSERT INTO resumes (candidate_profile_id, original_filename, storage_ref, mime_type, is_master)
                VALUES (?, 'a.pdf', 'ref-a', 'application/pdf', true)
                """, candidate);
        // second master for same candidate must violate ux_resumes_one_master
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO resumes (candidate_profile_id, original_filename, storage_ref, mime_type, is_master)
                VALUES (?, 'b.pdf', 'ref-b', 'application/pdf', true)
                """, candidate))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // a non-master row for the same candidate is allowed
        jdbc.update("""
                INSERT INTO resumes (candidate_profile_id, original_filename, storage_ref, mime_type, is_master)
                VALUES (?, 'c.pdf', 'ref-c', 'application/pdf', false)
                """, candidate);
        Long count = jdbc.queryForObject(
                "SELECT count(*) FROM resumes WHERE candidate_profile_id = ?", Long.class, candidate);
        assertThat(count).isEqualTo(2);
    }

    @Test
    void skillEvidenceConfidenceMustBeBetweenZeroAndOne() {
        UUID candidate = newCandidate();
        UUID skill = jdbc.queryForObject("""
                INSERT INTO skills (candidate_profile_id, name, normalized_name)
                VALUES (?, 'Java', 'java') RETURNING id
                """, UUID.class, candidate);

        // 0.7 is valid
        jdbc.update("""
                INSERT INTO skill_evidence (skill_id, source_type, source_id, excerpt, confidence)
                VALUES (?, 'PROJECT', gen_random_uuid(), 'built a service', 0.7)
                """, skill);

        // 1.5 violates the CHECK constraint
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO skill_evidence (skill_id, source_type, source_id, excerpt, confidence)
                VALUES (?, 'PROJECT', gen_random_uuid(), 'excerpt', 1.5)
                """, skill))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);

        // -0.1 also violates
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO skill_evidence (skill_id, source_type, source_id, excerpt, confidence)
                VALUES (?, 'PROJECT', gen_random_uuid(), 'excerpt', -0.1)
                """, skill))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    private UUID newCandidate() {
        UUID userId = jdbc.queryForObject("""
                INSERT INTO users (email) VALUES (gen_random_uuid() || '@test.dev') RETURNING id
                """, UUID.class);
        return jdbc.queryForObject("""
                INSERT INTO candidate_profiles (user_id) VALUES (?) RETURNING id
                """, UUID.class, userId);
    }
}

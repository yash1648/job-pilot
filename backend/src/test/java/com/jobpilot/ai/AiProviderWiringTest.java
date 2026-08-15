package com.jobpilot.ai;

import com.jobpilot.ai.provider.fake.FakeAiService;
import com.jobpilot.ai.provider.fake.FakeEmbeddingService;
import com.jobpilot.ai.provider.fake.FakeVisionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Verifies the conditional provider wiring: with {@code jobpilot.ai.provider=fake}
 * the fake implementations are selected (doc 06 §1, doc 26 §3). Also confirms
 * the full context loads with the ai module present.
 */
@Testcontainers
@SpringBootTest(properties = "jobpilot.ai.provider=fake")
class AiProviderWiringTest {

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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    AiService aiService;

    @Autowired
    EmbeddingService embeddingService;

    @Autowired
    VisionService visionService;

    @Test
    void fakeProviderIsWiredInTestProfile() {
        assertInstanceOf(FakeAiService.class, aiService);
        assertInstanceOf(FakeEmbeddingService.class, embeddingService);
        assertInstanceOf(FakeVisionService.class, visionService);
    }
}

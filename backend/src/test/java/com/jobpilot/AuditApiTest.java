package com.jobpilot;

import com.jobpilot.audit.api.AuditDtos.AuditEventRequest;
import com.jobpilot.audit.api.AuditService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.dao.DataAccessException;

/**
 * Audit trail API + append-only enforcement (doc 05 §9, doc 22 §10).
 * Boots the full context against a real Postgres so the V4 append-only trigger
 * is exercised end-to-end.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuditApiTest {

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
    MockMvc mockMvc;

    @Autowired
    AuditService auditService;

    @Autowired
    JdbcTemplate jdbc;

    final ObjectMapper objectMapper = new ObjectMapper();

    String email;
    String password = "correct-horse-battery";

    @BeforeEach
    void uniqueEmail() {
        email = "audit" + System.nanoTime() + "@test.dev";
    }

    @Test
    void userSeesOnlyOwnAuditTrail() throws Exception {
        Auth u1 = authed();
        auditService.record(new AuditEventRequest("USER", u1.userId, "LOGIN", "user", u1.userId,
                Map.of("ip", "1.2.3.4")));
        auditService.record(new AuditEventRequest("USER", u1.userId, "PROFILE_UPDATE", "candidate_profile", "x",
                Map.of("field", "headline")));

        // user1 sees exactly their 2 events
        mockMvc.perform(get("/api/v1/settings/audit").header("Authorization", u1.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(2));

        // user2 sees none of user1's events
        Auth u2 = authed("other" + System.nanoTime() + "@test.dev");
        mockMvc.perform(get("/api/v1/settings/audit").header("Authorization", u2.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(0));
    }

    @Test
    void unauthenticatedAuditRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/settings/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auditEventsAreAppendOnly() {
        // The V4 trigger must reject any mutation of audit_events (doc 22 §10).
        assertThrows(DataAccessException.class, () -> jdbc.update("DELETE FROM audit_events"));
        assertThrows(DataAccessException.class, () -> jdbc.update("UPDATE audit_events SET event_type = 'X'"));
    }

    // --- helpers ---

    private static final class Auth {
        String token;
        String userId;
    }

    private Auth authed() throws Exception {
        return authed(email);
    }

    private Auth authed(String regEmail) throws Exception {
        MvcResult reg = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + regEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = read(reg, "$.id");

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + regEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = read(login, "$.accessToken");

        Auth a = new Auth();
        a.token = "Bearer " + accessToken;
        a.userId = userId;
        return a;
    }

    private String read(MvcResult result, String jsonPath) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String part : jsonPath.replace("$.", "").split("\\.")) {
            node = node.get(part);
        }
        return node.asText();
    }
}

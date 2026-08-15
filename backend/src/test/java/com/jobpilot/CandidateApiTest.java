package com.jobpilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Candidate profile + preferences API tests (doc 05 §2/§3, doc 22 §2 scoping,
 * doc 25 §5 no cross-candidate leakage). Boots the full context with a real
 * JWT (register/login) so the security filter chain is exercised end-to-end.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class CandidateApiTest {

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

    final ObjectMapper objectMapper = new ObjectMapper();

    String email;
    String password = "correct-horse-battery";

    @BeforeEach
    void uniqueEmail() {
        email = "cand" + System.nanoTime() + "@test.dev";
    }

    @Test
    void profileRoundTripAndPatch() throws Exception {
        String token = authedToken();

        // no profile yet → 404
        mockMvc.perform(get("/api/v1/candidate/profile").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        // PATCH creates it (upsert)
        mockMvc.perform(patch("/api/v1/candidate/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Senior Backend Engineer\",\"strengths\":[\"Java\",\"Distributed Systems\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Senior Backend Engineer"))
                .andExpect(jsonPath("$.strengths[0]").value("Java"));

        // GET returns it
        mockMvc.perform(get("/api/v1/candidate/profile").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Senior Backend Engineer"));

        // PATCH updates only provided fields
        mockMvc.perform(patch("/api/v1/candidate/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"Staff Engineer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headline").value("Staff Engineer"))
                .andExpect(jsonPath("$.strengths[0]").value("Java"));
    }

    @Test
    void preferencesRoundTripAndInvalidEnumRejected() throws Exception {
        String token = authedToken();
        // need a profile before preferences exist
        mockMvc.perform(patch("/api/v1/candidate/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"x\"}"))
                .andExpect(status().isOk());

        // no preferences yet → 404
        mockMvc.perform(get("/api/v1/preferences").header("Authorization", token))
                .andExpect(status().isNotFound());

        // PUT valid preferences
        mockMvc.perform(put("/api/v1/preferences")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetRoles\":[\"SRE\"],\"locations\":[\"Berlin\"],"
                                + "\"workMode\":\"REMOTE\",\"automationMode\":\"FULLY_MANUAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workMode").value("REMOTE"))
                .andExpect(jsonPath("$.automationMode").value("FULLY_MANUAL"));

        // GET returns them
        mockMvc.perform(get("/api/v1/preferences").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetRoles[0]").value("SRE"));

        // invalid enum → 400 VALIDATION_ERROR (doc 05 §12)
        mockMvc.perform(put("/api/v1/preferences")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workMode\":\"TELEPORT\",\"automationMode\":\"FULLY_MANUAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void crossCandidateAccessReturns404() throws Exception {
        // user A creates a profile
        String tokenA = authedToken();
        mockMvc.perform(patch("/api/v1/candidate/profile")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"A's profile\"}"))
                .andExpect(status().isOk());

        // user B (separate account) must NOT see A's data
        String otherEmail = "other" + System.nanoTime() + "@test.dev";
        String tokenB = authedToken(otherEmail);
        mockMvc.perform(get("/api/v1/candidate/profile").header("Authorization", tokenB))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/preferences").header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void unauthenticatedCandidateRequestIs401() throws Exception {
        mockMvc.perform(get("/api/v1/candidate/profile"))
                .andExpect(status().isUnauthorized());
    }

    // --- helpers ---

    private String authedToken() throws Exception {
        return authedToken(email);
    }

    private String authedToken(String regEmail) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + regEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + regEmail + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return "Bearer " + read(login, "$.accessToken");
    }

    private String read(MvcResult result, String jsonPath) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        for (String part : jsonPath.replace("$.", "").split("\\.")) {
            node = node.get(part);
        }
        return node.asText();
    }
}

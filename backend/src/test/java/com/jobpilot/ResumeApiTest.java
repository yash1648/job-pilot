package com.jobpilot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resume upload + storage integration API tests (doc 05 §2, doc 07 §2, doc 22 §4).
 * Boots the full context with a real JWT so the security filter chain and the
 * StorageService (encrypted, content-sniffed) are exercised end-to-end.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ResumeApiTest {

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
        email = "res" + System.nanoTime() + "@test.dev";
    }

    @Test
    void uploadPdfReturns202WithPendingParseStatus() throws Exception {
        String token = authedToken();
        ensureProfile(token);

        MockMultipartFile pdf = new MockMultipartFile("resume", "cv.pdf",
                "application/pdf", "%PDF-1.4 fake but sniffable content".getBytes());

        MvcResult r = mockMvc.perform(multipart("/api/v1/candidate/resumes").file(pdf)
                        .header("Authorization", token))
                .andExpect(status().isAccepted())
                .andReturn();

        JsonNode node = objectMapper.readTree(r.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", node.get("parseStatus").asText());
        String id = node.get("id").asText();

        // detail reflects PENDING
        mockMvc.perform(get("/api/v1/candidate/resumes/" + id).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parseStatus").value("PENDING"))
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));
    }

    @Test
    void listAndDetailRoundTrip() throws Exception {
        String token = authedToken();
        ensureProfile(token);
        upload(token, "a.pdf");
        upload(token, "b.pdf");

        mockMvc.perform(get("/api/v1/candidate/resumes").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void setMasterThenDeleteMasterReturns409() throws Exception {
        String token = authedToken();
        ensureProfile(token);
        String id = upload(token, "cv.pdf");

        mockMvc.perform(post("/api/v1/candidate/resumes/" + id + "/set-master")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMaster").value(true));

        // deleting the master without a replacement is blocked (doc 05 §2)
        mockMvc.perform(delete("/api/v1/candidate/resumes/" + id).header("Authorization", token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void disallowedMimeReturns400() throws Exception {
        String token = authedToken();
        ensureProfile(token);

        MockMultipartFile txt = new MockMultipartFile("resume", "note.txt",
                "text/plain", "just some plain text, not a PDF or DOCX".getBytes());

        mockMvc.perform(multipart("/api/v1/candidate/resumes").file(txt)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_FILE"));
    }

    @Test
    void unauthenticatedUploadIs401() throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("resume", "cv.pdf",
                "application/pdf", "%PDF-1.4".getBytes());
        mockMvc.perform(multipart("/api/v1/candidate/resumes").file(pdf))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crossUserCannotAccessResume() throws Exception {
        String tokenA = authedToken();
        ensureProfile(tokenA);
        String id = upload(tokenA, "cv.pdf");

        String tokenB = authedToken("other" + System.nanoTime() + "@test.dev");
        mockMvc.perform(get("/api/v1/candidate/resumes/" + id).header("Authorization", tokenB))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---

    private String upload(String token, String filename) throws Exception {
        MockMultipartFile pdf = new MockMultipartFile("resume", filename,
                "application/pdf", ("%PDF-1.4 " + filename).getBytes());
        MvcResult r = mockMvc.perform(multipart("/api/v1/candidate/resumes").file(pdf)
                        .header("Authorization", token))
                .andExpect(status().isAccepted())
                .andReturn();
        return objectMapper.readTree(r.getResponse().getContentAsString()).get("id").asText();
    }

    private void ensureProfile(String token) throws Exception {
        mockMvc.perform(patch("/api/v1/candidate/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"headline\":\"x\"}"))
                .andExpect(status().isOk());
    }

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

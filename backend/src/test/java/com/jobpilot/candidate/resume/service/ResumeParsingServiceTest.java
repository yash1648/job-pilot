package com.jobpilot.candidate.resume.service;

import com.jobpilot.candidate.resume.TestResumeFixtures;
import com.jobpilot.candidate.resume.service.ResumeParsingService.ParsedResume;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ResumeParsingService unit tests (doc 07 §2, doc 22:39-41): clean text-PDF,
 * DOCX with tables, corrupt file → FAILED reason, zip-bomb and XXE DOCX
 * rejected. No Spring context, no DB.
 */
class ResumeParsingServiceTest {

    private final ResumeParsingService service = new ResumeParsingService();

    @Test
    void cleanTextPdfExtractsTextLayerWithoutOcr() throws Exception {
        ParsedResume r = service.parse(
                TestResumeFixtures.textPdf("Senior Java Engineer\nSpring Boot and PostgreSQL"),
                "application/pdf");
        assertTrue(r.text().contains("Senior Java Engineer"), r.text());
        assertTrue(r.text().contains("Spring Boot"), r.text());
        assertFalse(r.ocrUsed());
    }

    @Test
    void docxWithTableExtractsCellText() throws Exception {
        ParsedResume r = service.parse(
                TestResumeFixtures.docxWithTable(new String[][]{{"Spring Boot", "PostgreSQL"}, {"Kubernetes", "AWS"}}),
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertTrue(r.text().contains("Spring Boot"), r.text());
        assertTrue(r.text().contains("Kubernetes"), r.text());
        assertTrue(r.text().contains("AWS"), r.text());
    }

    @Test
    void corruptFileThrowsWithUserFacingReason() {
        ResumeParseException e = assertThrows(ResumeParseException.class,
                () -> service.parse(TestResumeFixtures.corruptPdf(), "application/pdf"));
        assertTrue(e.reason().contains("unreadable"), e.reason());
    }

    @Test
    void unsupportedMimeTypeThrows() {
        ResumeParseException e = assertThrows(ResumeParseException.class,
                () -> service.parse("hello".getBytes(), "text/plain"));
        assertTrue(e.reason().contains("unsupported"), e.reason());
    }

    @Test
    void zipBombDocxIsRejected() throws Exception {
        ResumeParseException e = assertThrows(ResumeParseException.class,
                () -> service.parse(TestResumeFixtures.zipBombDocx(),
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertTrue(e.reason().contains("zip bomb"), e.reason());
    }

    @Test
    void xxeDocxIsRejected() throws Exception {
        // the point is the external entity is never resolved (doc 22:39-41)
        assertThrows(ResumeParseException.class,
                () -> service.parse(TestResumeFixtures.xxeDocx(),
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }
}

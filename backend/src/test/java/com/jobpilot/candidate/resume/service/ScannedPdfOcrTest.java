package com.jobpilot.candidate.resume.service;

import com.jobpilot.candidate.resume.TestResumeFixtures;
import com.jobpilot.candidate.resume.service.ResumeParsingService.ParsedResume;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.LoadLibs;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OCR fallback for scanned PDFs (doc 07:18). Tagged {@code slow} and skipped
 * when the Tesseract native library is unavailable (Assumptions), mirroring the
 * Ollama integration test pattern — so the default suite never hard-fails on
 * environments without tesseract installed.
 */
@Tag("slow")
class ScannedPdfOcrTest {

    @Test
    void scannedPdfOcrRecoversText() throws Exception {
        Assumptions.assumeTrue(ocrAvailable(), "Tesseract native library not available");

        ParsedResume r = new ResumeParsingService().parse(
                TestResumeFixtures.scannedPdf("ZEBRA42"), "application/pdf");

        assertTrue(r.ocrUsed(), "expected the OCR path to be used");
        assertTrue(r.text().contains("ZEBRA42"), "OCR text: '" + r.text() + "'");
    }

    private static boolean ocrAvailable() {
        try {
            Tesseract t = new Tesseract();
            Path system = Path.of("/usr/share/tessdata");
            t.setDatapath(Files.isDirectory(system) ? system.toString()
                    : LoadLibs.extractTessResources("tessdata").getParent());
            t.doOCR(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }
}

package com.jobpilot.candidate.resume.service;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.util.LoadLibs;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.xml.sax.SAXException;

/**
 * Pure text/layout extraction from uploaded resumes (doc 07 §2).
 * <ul>
 *   <li>PDF: text layer first; scanned PDFs (no text layer) fall back to OCR
 *       via Tesseract (doc 07:18).</li>
 *   <li>DOCX: structured XML parsing (word/document.xml) with stdlib zip + a
 *       hardened XML parser — zip-bomb cap and XXE disabled (doc 22:39-41).</li>
 * </ul>
 * Failures throw {@link ResumeParseException} with a user-facing reason; the
 * caller records {@code parse_status=FAILED} (doc 07:36-38).
 */
@Service
public class ResumeParsingService {

    /** Zip-bomb cap: aborts if word/document.xml inflates beyond this (doc 22:40). */
    private static final long MAX_DOCX_XML_BYTES = 20L * 1024 * 1024;
    private static final int OCR_DPI = 300;
    private static final int MAX_OCR_PAGES = 20;
    private static final String WML_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

    /** Extraction result. {@code lowConfidenceOcr} flags scanned pages that OCR'd to empty. */
    public record ParsedResume(String text, boolean ocrUsed, boolean lowConfidenceOcr, List<String> warnings) {
    }

    public ParsedResume parse(byte[] content, String mimeType) {
        if (content == null || content.length == 0) {
            throw new ResumeParseException("empty file");
        }
        if (mimeType == null || mimeType.isBlank()) {
            throw new ResumeParseException("unsupported file type");
        }
        if (mimeType.contains("pdf")) {
            return parsePdf(content);
        }
        if (mimeType.contains("openxmlformats") || mimeType.contains("wordprocessingml")) {
            return parseDocx(content);
        }
        throw new ResumeParseException("unsupported file type: " + mimeType);
    }

    private ParsedResume parsePdf(byte[] content) {
        String text;
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        } catch (IOException e) {
            throw new ResumeParseException("unreadable PDF file");
        }
        if (text != null && !text.isBlank()) {
            return new ParsedResume(text, false, false, List.of());
        }
        // no text layer → likely scanned → OCR fallback (doc 07:18)
        return ocrPdf(content);
    }

    private ParsedResume ocrPdf(byte[] content) {
        Tesseract tess = tesseract();
        StringBuilder sb = new StringBuilder();
        List<String> warnings = new ArrayList<>();
        boolean anyLowConfidence = false;
        try (PDDocument doc = Loader.loadPDF(content)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            int pages = Math.min(doc.getNumberOfPages(), MAX_OCR_PAGES);
            if (doc.getNumberOfPages() > MAX_OCR_PAGES) {
                warnings.add("resume has more than " + MAX_OCR_PAGES
                        + " pages; OCR applied to the first " + MAX_OCR_PAGES);
            }
            for (int i = 0; i < pages; i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, OCR_DPI);
                String pageText = tess.doOCR(img);
                if (pageText != null && pageText.isBlank()) {
                    anyLowConfidence = true; // doc 07:101 — proceed but flag
                }
                sb.append(pageText == null ? "" : pageText).append('\n');
            }
        } catch (IOException e) {
            throw new ResumeParseException("unreadable scanned PDF");
        } catch (TesseractException e) {
            throw new ResumeParseException("OCR failed: " + e.getMessage());
        } catch (UnsatisfiedLinkError e) {
            throw new ResumeParseException("scanned PDF detected but the OCR engine is not available on this server");
        }
        String text = sb.toString().trim();
        if (text.isEmpty()) {
            throw new ResumeParseException("scanned PDF produced no readable text (OCR failed)");
        }
        return new ParsedResume(text, true, anyLowConfidence, warnings);
    }

    private ParsedResume parseDocx(byte[] content) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            byte[] docXml = null;
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    docXml = readCapped(zip, MAX_DOCX_XML_BYTES);
                    break;
                }
            }
            if (docXml == null) {
                throw new ResumeParseException("DOCX has no word/document.xml");
            }
            String text = extractTextFromDocXml(docXml);
            if (text.isBlank()) {
                throw new ResumeParseException("DOCX produced no readable text");
            }
            return new ParsedResume(text, false, false, List.of());
        } catch (IOException e) {
            throw new ResumeParseException("unreadable DOCX file");
        }
    }

    /** Reads an entry, aborting if it inflates beyond {@code cap} (zip-bomb, doc 22:40). */
    private static byte[] readCapped(InputStream in, long cap) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buf)) != -1) {
            total += n;
            if (total > cap) {
                throw new ResumeParseException("DOCX content exceeds the size limit (possible zip bomb)");
            }
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    /** XXE-hardened walk of word/document.xml collecting all {@code w:t} text (doc 22:39-41). */
    private static String extractTextFromDocXml(byte[] xml) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // hard-disable DTDs and external entities (XXE)
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            dbf.setXIncludeAware(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(xml));
            NodeList texts = doc.getElementsByTagNameNS(WML_NS, "t");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < texts.getLength(); i++) {
                sb.append(texts.item(i).getTextContent());
            }
            return sb.toString().trim();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ResumeParseException("DOCX XML is invalid or unsafe");
        }
    }

    private static Tesseract tesseract() {
        Tesseract tess = new Tesseract();
        tess.setLanguage("eng");
        // Linux: prefer the system tessdata (CI installs tesseract-ocr); fall
        // back to the eng.traineddata bundled inside the tess4j jar.
        Path system = Path.of("/usr/share/tessdata");
        if (Files.isDirectory(system)) {
            tess.setDatapath(system.toString());
        } else {
            tess.setDatapath(LoadLibs.extractTessResources("tessdata").getParent());
        }
        return tess;
    }
}

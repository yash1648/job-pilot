package com.jobpilot.candidate.resume;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Programmatic resume fixtures (doc 26 §2) — real PDF/DOCX bytes generated in
 * memory so no binaries live in the repo: clean text-PDF, scanned (image-only)
 * PDF, DOCX with a table, corrupt PDF, zip-bomb DOCX, XXE DOCX.
 */
public final class TestResumeFixtures {

    private TestResumeFixtures() {
    }

    private static final String CONTENT_TYPES = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
              <Default Extension="xml" ContentType="application/xml"/>
              <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
            </Types>
            """;

    private static final String RELS = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
              <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
            </Relationships>
            """;

    /** A PDF with a real text layer (no OCR needed). */
    public static byte[] textPdf(String body) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(50, 700);
                for (String line : body.split("\n")) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -14);
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** An image-only PDF (no text layer) — exercises the OCR fallback path. */
    public static byte[] scannedPdf(String text) throws IOException {
        BufferedImage img = new BufferedImage(1200, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 1200, 300);
        g.setColor(Color.BLACK);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        g.drawString(text, 40, 180);
        g.dispose();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(img.getWidth(), img.getHeight()));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                PDImageXObject ximg = LosslessFactory.createFromImage(doc, img);
                cs.drawImage(ximg, 0, 0);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    /** A DOCX whose body contains the given rows as a table of single-cell paragraphs. */
    public static byte[] docxWithTable(String[][] rows) throws IOException {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body>
                """);
        for (String[] row : rows) {
            xml.append("    <w:tbl><w:tr>");
            for (String cell : row) {
                xml.append("<w:tc><w:p><w:r><w:t>").append(cell).append("</w:t></w:r></w:p></w:tc>");
            }
            xml.append("</w:tr></w:tbl>\n");
        }
        xml.append("  </w:body>\n</w:document>");
        return docx(xml.toString());
    }

    /** A DOCX whose document.xml contains a DOCTYPE with an external entity (XXE probe). */
    public static byte[] xxeDocx() throws IOException {
        String xml = """
                <?xml version="1.0"?>
                <!DOCTYPE w:document [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
                  <w:body><w:p><w:r><w:t>&xxe;</w:t></w:r></w:p></w:body>
                </w:document>
                """;
        return docx(xml);
    }

    /** A DOCX whose document.xml inflates far beyond the parser cap (zip bomb). */
    public static byte[] zipBombDocx() throws IOException {
        String xml = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body><w:p><w:r><w:t>"
                + "A".repeat(30 * 1024 * 1024) // 30MB of 'A' compresses to a few KB
                + "</w:t></w:r></w:p></w:body></w:document>";
        return docx(xml);
    }

    /** Corrupt bytes that are not a valid PDF. */
    public static byte[] corruptPdf() {
        return "this is definitely not a pdf".getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] docx(String documentXml) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
            zip.write(CONTENT_TYPES.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("_rels/.rels"));
            zip.write(RELS.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return out.toByteArray();
    }
}

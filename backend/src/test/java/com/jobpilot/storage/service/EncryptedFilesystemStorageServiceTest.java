package com.jobpilot.storage.service;

import com.jobpilot.storage.api.StorageDtos.StoredFile;
import com.jobpilot.storage.api.StorageDtos.StoreRequest;
import com.jobpilot.storage.api.StorageService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Storage module tests (doc 22 §4/§5, doc 25 §8): content-sniffing MIME
 * allow-list, size limit, encryption-at-rest, and path-traversal guard.
 */
class EncryptedFilesystemStorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService service() {
        return new EncryptedFilesystemStorageService(
                tempDir.toString(), 1024, "test-key-not-for-prod");
    }

    private static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes();
    }

    private static byte[] docx() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(baos)) {
            ZipEntry e = new ZipEntry("[Content_Types].xml");
            z.putNextEntry(e);
            z.write("<Types/>".getBytes());
            z.closeEntry();
        }
        return baos.toByteArray();
    }

    @Test
    void storesAndRetrievesPdf() {
        StorageService s = service();
        StoredFile f = s.store(new StoreRequest(pdf(), "cv.pdf", "application/pdf", UUID.randomUUID(), "RESUME"));
        assertEquals("application/pdf", f.mimeType());
        assertArrayEquals(pdf(), s.retrieve(f.storageRef()));
    }

    @Test
    void storesAndRetrievesDocx() throws Exception {
        StorageService s = service();
        StoredFile f = s.store(new StoreRequest(docx(), "cv.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", UUID.randomUUID(), "RESUME"));
        assertTrue(f.mimeType().contains("officedocument"));
        assertArrayEquals(docx(), s.retrieve(f.storageRef()));
    }

    @Test
    void fileOnDiskIsEncryptedNotPlaintext() throws Exception {
        StorageService s = service();
        byte[] content = pdf();
        StoredFile f = s.store(new StoreRequest(content, "cv.pdf", "application/pdf", UUID.randomUUID(), "RESUME"));
        byte[] raw = Files.readAllBytes(tempDir.resolve(f.storageRef()));
        // encrypted blob must not start with the PDF magic and must be larger
        // than the plaintext (IV + GCM tag)
        assertFalse(new String(raw, 0, Math.min(4, raw.length)).startsWith("%PDF"));
        assertTrue(raw.length > content.length);
    }

    @Test
    void disallowedMimeRejected() {
        StorageService s = service();
        assertThrows(IllegalArgumentException.class,
                () -> s.store(new StoreRequest("just text".getBytes(), "note.txt", "text/plain", UUID.randomUUID(), "RESUME")));
    }

    @Test
    void renamedExeToPdfRejectedBySniffing() {
        // content is a PE header (MZ), filename lies about being a PDF
        byte[] exe = new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00,
                't', 'h', 'i', 's', '-', 'i', 's', '-', 'n', 'o', 't', '-', 'a', '-', 'p', 'd', 'f'};
        StorageService s = service();
        assertThrows(IllegalArgumentException.class,
                () -> s.store(new StoreRequest(exe, "evil.pdf", "application/pdf", UUID.randomUUID(), "RESUME")));
    }

    @Test
    void oversizedRejected() {
        StorageService s = service(); // max 1024 bytes
        byte[] big = new byte[2048];
        assertThrows(IllegalArgumentException.class,
                () -> s.store(new StoreRequest(big, "big.pdf", "application/pdf", UUID.randomUUID(), "RESUME")));
    }

    @Test
    void deleteRemovesStoredBytes() {
        StorageService s = service();
        StoredFile f = s.store(new StoreRequest(pdf(), "cv.pdf", "application/pdf", UUID.randomUUID(), "RESUME"));
        s.delete(f.storageRef());
        assertTrue(Files.notExists(tempDir.resolve(f.storageRef())));
    }

    @Test
    void pathTraversalRefRejected() {
        StorageService s = service();
        assertThrows(IllegalArgumentException.class, () -> s.retrieve("../escape"));
    }
}

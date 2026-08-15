package com.jobpilot.storage.service;

import com.jobpilot.storage.api.StorageDtos.StoredFile;
import com.jobpilot.storage.api.StorageDtos.StoreRequest;
import com.jobpilot.storage.api.StorageService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Encrypted filesystem storage (doc 22 §4/§5). Hardening:
 * <ul>
 *   <li>MIME allow-list (PDF, DOCX) enforced by <b>content-sniffing</b>, never
 *       the filename extension (doc 22 §4, doc 25 §8).</li>
 *   <li>Size limit enforced before any bytes hit disk.</li>
 *   <li>Bytes encrypted at rest with AES/GCM; the key is derived from
 *       {@code jobpilot.storage.encryption-key} (a passphrase hashed to 32
 *       bytes) — never embedded in source (doc 22 §5).</li>
 *   <li>Stored outside the web root; only reachable via this service. The
 *       {@code storageRef} is an opaque {@code owner/uuid} key, not a path.</li>
 * </ul>
 * Rejects (IllegalArgumentException) anything that fails sniffing or exceeds
 * the size limit, so malicious uploads never reach storage (doc 25 §1/§8).
 */
@Service
public class EncryptedFilesystemStorageService implements StorageService {

    private static final long DEFAULT_MAX_BYTES = 10L * 1024 * 1024;
    private static final int IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final byte[] ZIP_MAGIC = {0x50, 0x4B, 0x03, 0x04}; // PK..
    private static final byte[] DOCX_MARKER = "[Content_Types].xml".getBytes(StandardCharsets.US_ASCII);

    private final Path root;
    private final long maxSizeBytes;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public EncryptedFilesystemStorageService(
            @Value("${jobpilot.storage.root:./jobpilot-storage}") String root,
            @Value("${jobpilot.storage.max-size-bytes:10485760}") long maxSizeBytes,
            @Value("${jobpilot.storage.encryption-key:dev-only-insecure-key-please-change}") String encryptionKey) {
        this.root = Path.of(root);
        this.maxSizeBytes = maxSizeBytes;
        this.key = new SecretKeySpec(sha256(encryptionKey), "AES");
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("cannot create storage root: " + this.root, e);
        }
    }

    @Override
    public StoredFile store(StoreRequest req) {
        byte[] content = req.content();
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("empty file");
        }
        if (content.length > maxSizeBytes) {
            throw new IllegalArgumentException(
                    "file too large: " + content.length + " > " + maxSizeBytes);
        }
        String mime = sniff(content);
        if (mime == null) {
            throw new IllegalArgumentException("unsupported content (allowed: PDF, DOCX)");
        }
        String ref = req.ownerId() + "/" + UUID.randomUUID();
        Path file = root.resolve(ref);
        try {
            Files.createDirectories(file.getParent());
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            byte[] cipher = aes(Cipher.ENCRYPT_MODE, content, iv);
            byte[] out = new byte[IV_BYTES + cipher.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(cipher, 0, out, IV_BYTES, cipher.length);
            Files.write(file, out);
        } catch (IOException e) {
            throw new IllegalStateException("storage write failed", e);
        }
        return new StoredFile(ref, mime, content.length, req.originalFilename());
    }

    @Override
    public byte[] retrieve(String storageRef) {
        Path file = resolve(storageRef);
        try {
            byte[] out = Files.readAllBytes(file);
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(out, 0, iv, 0, IV_BYTES);
            byte[] cipher = new byte[out.length - IV_BYTES];
            System.arraycopy(out, IV_BYTES, cipher, 0, cipher.length);
            return aes(Cipher.DECRYPT_MODE, cipher, iv);
        } catch (IOException e) {
            throw new IllegalStateException("storage read failed: " + storageRef, e);
        }
    }

    @Override
    public void delete(String storageRef) {
        try {
            Files.deleteIfExists(resolve(storageRef));
        } catch (IOException e) {
            throw new IllegalStateException("storage delete failed: " + storageRef, e);
        }
    }

    private Path resolve(String ref) {
        // guard against path traversal: ref must stay under root
        Path p = root.resolve(ref).normalize();
        if (!p.startsWith(root.normalize())) {
            throw new IllegalArgumentException("invalid storage ref");
        }
        return p;
    }

    /** Content-sniffing MIME detection (doc 22 §4). Returns null if unsupported. */
    private static String sniff(byte[] c) {
        if (c.length >= 4 && new String(c, 0, 4, StandardCharsets.US_ASCII).startsWith("%PDF")) {
            return "application/pdf";
        }
        if (startsWith(c, ZIP_MAGIC) && contains(c, DOCX_MARKER)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static boolean contains(byte[] data, byte[] sub) {
        if (data.length < sub.length) return false;
        for (int i = 0; i <= data.length - sub.length; i++) {
            boolean ok = true;
            for (int j = 0; j < sub.length; j++) {
                if (data[i + j] != sub[j]) { ok = false; break; }
            }
            if (ok) return true;
        }
        return false;
    }

    private byte[] aes(int mode, byte[] data, byte[] iv) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(mode, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("aes failed", e);
        }
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

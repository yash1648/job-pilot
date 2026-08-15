package com.jobpilot.storage.api;

import java.util.UUID;

/**
 * Request/response types for the storage module (doc 22 §4/§5).
 */
public final class StorageDtos {

    private StorageDtos() {
    }

    /** Upload request. {@code declaredContentType} is untrusted — the service
     *  re-derives the real type by content-sniffing (doc 22 §4). */
    public record StoreRequest(
            byte[] content,
            String originalFilename,
            String declaredContentType,
            UUID ownerId,
            String purpose) {
    }

    public record StoredFile(
            String storageRef,
            String mimeType,
            long sizeBytes,
            String originalFilename) {
    }
}

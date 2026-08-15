package com.jobpilot.storage.api;

import com.jobpilot.storage.api.StorageDtos.StoredFile;
import com.jobpilot.storage.api.StorageDtos.StoreRequest;

import java.util.UUID;

/**
 * Storage module public API (doc 22 §4/§5). Cross-module callers depend on
 * {@link StorageService} only — never on {@code storage.service} internals
 * (doc 34 §3). Files are stored encrypted outside the web root and are only
 * ever reachable through this service; the returned {@code storageRef} is an
 * opaque key, never a web-served path.
 */
public interface StorageService {

    /** Encrypt + persist {@code content}; returns an opaque ref for later retrieval. */
    StoredFile store(StoreRequest request);

    /** Decrypt + return the bytes previously stored under {@code storageRef}. */
    byte[] retrieve(String storageRef);

    /** Remove the stored (encrypted) bytes for {@code storageRef}. */
    void delete(String storageRef);
}

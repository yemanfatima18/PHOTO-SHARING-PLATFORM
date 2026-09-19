package com.photoshare.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

public interface StorageService {

    /**
     * Stores a file and returns the storage key (relative path).
     */
    String store(MultipartFile file, UUID eventId, String originalFilename) throws StorageException;

    /**
     * Stores a file from an input stream and returns the storage key.
     */
    String store(InputStream inputStream, String contentType, long contentLength, UUID eventId, String originalFilename) throws StorageException;

    /**
     * Loads a file as a Resource for downloading/serving.
     */
    Resource loadAsResource(String storageKey) throws StorageException;

    /**
     * Checks if a file exists.
     */
    boolean exists(String storageKey);

    /**
     * Deletes a file.
     */
    void delete(String storageKey) throws StorageException;

    /**
     * Generates a storage key for a file.
     */
    String generateStorageKey(UUID eventId, String originalFilename);

    /**
     * Gets the full file path for a storage key.
     */
    String getFilePath(String storageKey);

    /**
     * Validates if the content type is allowed.
     */
    boolean isAllowedContentType(String contentType);

    /**
     * Validates file size.
     */
    void validateFileSize(long fileSize) throws StorageException;

    /**
     * Validates a multipart file (content type, size, not empty).
     */
    void validateFile(org.springframework.web.multipart.MultipartFile file) throws StorageException;
}
package com.photoshare.storage;

import com.photoshare.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private final Path rootLocation;
    private final long maxFileSize;
    private final List<String> allowedContentTypes;

    public LocalStorageService(AppProperties appProperties) {
        this.rootLocation = Paths.get(appProperties.getStorage().getPath()).toAbsolutePath().normalize();
        this.maxFileSize = appProperties.getStorage().getMaxFileSize();
        this.allowedContentTypes = Arrays.asList(appProperties.getStorage().getAllowedContentTypes().split(","));

        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location: " + rootLocation, e);
        }
    }

    @Override
    public String store(MultipartFile file, UUID eventId, String originalFilename) throws StorageException {
        validateFileInternal(file);
        String storageKey = generateStorageKey(eventId, originalFilename);
        Path destinationFile = getPath(storageKey);

        try {
            Files.createDirectories(destinationFile.getParent());
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + storageKey, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String contentType, long contentLength, UUID eventId, String originalFilename) throws StorageException {
        validateContentType(contentType);
        if (contentLength > maxFileSize) {
            throw new StorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }

        String storageKey = generateStorageKey(eventId, originalFilename);
        Path destinationFile = getPath(storageKey);

        try {
            Files.createDirectories(destinationFile.getParent());
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + storageKey, e);
        }
    }

    @Override
    public Resource loadAsResource(String storageKey) throws StorageException {
        Path file = getPath(storageKey);
        try {
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("File not found or not readable: " + storageKey);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Error reading file: " + storageKey, e);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.exists(getPath(storageKey));
    }

    @Override
    public void delete(String storageKey) throws StorageException {
        Path file = getPath(storageKey);
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new StorageException("Failed to delete file: " + storageKey, e);
        }
    }

    @Override
    public String generateStorageKey(UUID eventId, String originalFilename) {
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalFilename.substring(lastDot).toLowerCase();
        }
        return "events/" + eventId + "/" + UUID.randomUUID() + extension;
    }

    @Override
    public String getFilePath(String storageKey) {
        return getPath(storageKey).toString();
    }

    @Override
    public boolean isAllowedContentType(String contentType) {
        return allowedContentTypes.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType.trim()));
    }

    @Override
    public void validateFileSize(long fileSize) throws StorageException {
        if (fileSize > maxFileSize) {
            throw new StorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
    }

    @Override
    public void validateFile(MultipartFile file) throws StorageException {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        validateContentType(file.getContentType());
        if (file.getSize() > maxFileSize) {
            throw new StorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
    }

    private void validateFileInternal(MultipartFile file) throws StorageException {
        if (file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }
        validateContentType(file.getContentType());
        if (file.getSize() > maxFileSize) {
            throw new StorageException("File size exceeds maximum allowed size of " + maxFileSize + " bytes");
        }
    }

    private void validateContentType(String contentType) throws StorageException {
        if (!isAllowedContentType(contentType)) {
            throw new StorageException("File type not allowed. Allowed types: " + String.join(", ", allowedContentTypes));
        }
    }

    private Path getPath(String storageKey) {
        return rootLocation.resolve(storageKey).normalize();
    }
}
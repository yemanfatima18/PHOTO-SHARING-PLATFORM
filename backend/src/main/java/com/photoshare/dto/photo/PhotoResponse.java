package com.photoshare.dto.photo;

import com.photoshare.entity.Photo;
import com.photoshare.entity.User;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PhotoResponse {

    private String id;
    private String eventId;
    private String uploadedById;
    private String uploadedByName;
    private String originalFilename;
    private String storageKey;
    private Long fileSize;
    private String contentType;
    private Boolean selectedForPublishing;
    private Instant createdAt;
    private String signedUrl;

    public static PhotoResponse from(Photo photo) {
        PhotoResponse response = new PhotoResponse();
        response.setId(photo.getId().toString());
        response.setEventId(photo.getEventId().toString());
        response.setUploadedById(photo.getUploadedBy().toString());
        response.setUploadedByName("Unknown"); // Would need to fetch user separately
        response.setOriginalFilename(photo.getOriginalFilename());
        response.setStorageKey(photo.getStorageKey());
        response.setFileSize(photo.getFileSize());
        response.setContentType(photo.getContentType());
        response.setSelectedForPublishing(photo.getSelectedForPublishing());
        response.setCreatedAt(photo.getCreatedAt());
        return response;
    }

    public static PhotoResponse fromWithSignedUrl(Photo photo, String signedUrl) {
        PhotoResponse response = from(photo);
        response.setSignedUrl(signedUrl);
        return response;
    }
}
package com.photoshare.dto.gallery;

import com.photoshare.entity.Gallery;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class GalleryResponse {

    private String id;
    private String eventId;
    private String eventName;
    private String publicToken;
    private Boolean published;
    private Instant publishedAt;
    private Instant createdAt;
    private Instant updatedAt;
    private int photoCount;
    private String shareUrl;

    public static GalleryResponse from(Gallery gallery, String frontendUrl, int photoCount, String eventName) {
        GalleryResponse response = new GalleryResponse();
        response.setId(gallery.getId().toString());
        response.setEventId(gallery.getEventId().toString());
        response.setEventName(eventName != null ? eventName : "Event");
        response.setPublicToken(gallery.getPublicToken());
        response.setPublished(gallery.getPublished());
        response.setPublishedAt(gallery.getPublishedAt());
        response.setCreatedAt(gallery.getCreatedAt());
        response.setUpdatedAt(gallery.getUpdatedAt());
        response.setPhotoCount(photoCount);
        if (frontendUrl != null && gallery.getPublicToken() != null) {
            response.setShareUrl(frontendUrl + "/gallery/" + gallery.getPublicToken());
        }
        return response;
    }

    @Data
    public static class PublicGalleryInfo {
        private String eventName;
        private Instant eventDate;
        private String galleryToken;
        private boolean requiresPin;
        private int photoCount;
    }
}
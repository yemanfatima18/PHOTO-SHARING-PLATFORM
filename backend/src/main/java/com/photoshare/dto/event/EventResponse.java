package com.photoshare.dto.event;

import com.photoshare.entity.Event;
import com.photoshare.entity.User;
import com.photoshare.repository.UserRepository;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EventResponse {

    private String id;
    private String name;
    private String description;
    private Instant eventDate;
    private String createdById;
    private String createdByName;
    private Instant createdAt;
    private Instant updatedAt;
    private long photoCount;
    private long selectedPhotoCount;
    private boolean hasGallery;
    private boolean galleryPublished;

    public static EventResponse from(Event event, long photoCount, long selectedPhotoCount, boolean hasGallery, boolean galleryPublished, UserRepository userRepository) {
        EventResponse response = new EventResponse();
        response.setId(event.getId().toString());
        response.setName(event.getName());
        response.setDescription(event.getDescription());
        response.setEventDate(event.getEventDate());
        response.setCreatedById(event.getCreatedBy().toString());
        
        User creator = userRepository.findById(event.getCreatedBy()).orElse(null);
        response.setCreatedByName(creator != null ? creator.getName() : "Unknown");
        
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        response.setPhotoCount(photoCount);
        response.setSelectedPhotoCount(selectedPhotoCount);
        response.setHasGallery(hasGallery);
        response.setGalleryPublished(galleryPublished);
        return response;
    }

    public static EventResponse from(Event event, UserRepository userRepository) {
        return from(event, 0, 0, false, false, userRepository);
    }
}
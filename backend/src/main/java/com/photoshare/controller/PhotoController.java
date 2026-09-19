package com.photoshare.controller;

import com.photoshare.dto.common.PageResponse;
import com.photoshare.dto.photo.PhotoResponse;
import com.photoshare.dto.photo.PhotoSelectionRequest;
import com.photoshare.entity.Event;
import com.photoshare.entity.User;
import com.photoshare.service.EventService;
import com.photoshare.service.PhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events/{eventId}/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;
    private final EventService eventService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PhotoResponse> uploadPhoto(
            @PathVariable UUID eventId,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws IOException {
        User user = (User) authentication.getPrincipal();
        eventService.verifyMemberAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        PhotoResponse response = photoService.uploadPhoto(event, user, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<PhotoResponse>> getEventPhotos(
            @PathVariable UUID eventId,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        Page<PhotoResponse> photos = photoService.getEventPhotosPaged(event, user, pageable);
        return ResponseEntity.ok(PageResponse.from(photos));
    }

    @GetMapping("/mine")
    public ResponseEntity<PageResponse<PhotoResponse>> getMyPhotos(
            @PathVariable UUID eventId,
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyMemberAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        Page<PhotoResponse> photos = photoService.getMyPhotosPaged(event, user, pageable);
        return ResponseEntity.ok(PageResponse.from(photos));
    }

    @PatchMapping("/selection")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateSelection(
            @PathVariable UUID eventId,
            @Valid @RequestBody PhotoSelectionRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        photoService.updateSelection(event, request.getPhotoIds(), request.getSelected(), user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/selected")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PhotoResponse>> getSelectedPhotos(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        List<PhotoResponse> photos = photoService.getSelectedPhotos(event);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/{photoId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> downloadPhoto(
            @PathVariable UUID eventId,
            @PathVariable UUID photoId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyMemberAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        try {
            org.springframework.core.io.Resource resource = photoService.loadPhotoAsResource(
                    photoService.findByIdAndEvent(photoId, event));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("Content-Disposition", "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            throw new com.photoshare.exception.ApiException(
                    org.springframework.http.HttpStatus.NOT_FOUND,
                    "NOT_FOUND",
                    "Photo not found"
            );
        }
    }
}
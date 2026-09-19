package com.photoshare.controller;

import com.photoshare.config.AppProperties;
import com.photoshare.dto.gallery.GalleryCreateRequest;
import com.photoshare.dto.gallery.GalleryResponse;
import com.photoshare.dto.gallery.PinVerifyRequest;
import com.photoshare.dto.gallery.PinVerifyResponse;
import com.photoshare.dto.gallery.PublicGalleryPhotoResponse;
import com.photoshare.entity.Event;
import com.photoshare.entity.User;
import com.photoshare.service.EventService;
import com.photoshare.service.GalleryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;
    private final EventService eventService;
    private final AppProperties appProperties;

    @PostMapping("/api/events/{eventId}/gallery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryResponse> createGallery(
            @PathVariable UUID eventId,
            @Valid @RequestBody GalleryCreateRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        GalleryResponse response = galleryService.createGallery(event, request, user, appProperties.getFrontendUrl());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/api/events/{eventId}/gallery")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryResponse> getGallery(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        GalleryResponse response = galleryService.getGallery(event, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/events/{eventId}/gallery/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryResponse> publishGallery(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        GalleryResponse response = galleryService.publishGallery(event, user, appProperties.getFrontendUrl());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/events/{eventId}/gallery/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unpublishGallery(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.verifyAdminAccess(eventId, user);

        Event event = new Event();
        event.setId(eventId);

        galleryService.unpublishGallery(event, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/public/galleries/{token}/verify")
    public ResponseEntity<PinVerifyResponse> verifyPin(
            @PathVariable String token,
            @Valid @RequestBody PinVerifyRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        PinVerifyResponse response = galleryService.verifyPin(token, request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/public/galleries/{token}")
    public ResponseEntity<GalleryResponse.PublicGalleryInfo> getPublicGalleryInfo(
            @PathVariable String token) {
        GalleryResponse.PublicGalleryInfo info = galleryService.getPublicGalleryInfo(token);
        return ResponseEntity.ok(info);
    }

    @GetMapping("/api/public/galleries/{token}/photos")
    public ResponseEntity<List<PublicGalleryPhotoResponse>> getPublicGalleryPhotos(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.photoshare.exception.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Missing or invalid authorization header"
            );
        }
        String accessToken = authHeader.substring(7);
        List<PublicGalleryPhotoResponse> photos = galleryService.getPublicPhotos(accessToken);
        return ResponseEntity.ok(photos);
    }

    @GetMapping("/api/public/galleries/{token}/photos/{photoId}")
    public ResponseEntity<Resource> getPublicGalleryPhoto(
            @PathVariable String token,
            @PathVariable UUID photoId,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.photoshare.exception.ApiException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "UNAUTHORIZED",
                    "Missing or invalid authorization header"
            );
        }
        String accessToken = authHeader.substring(7);
        
        Resource resource = galleryService.getPhotoResource(accessToken, photoId);
        
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(resource);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
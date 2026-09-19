package com.photoshare.service;

import com.photoshare.config.AppProperties;
import com.photoshare.dto.gallery.GalleryCreateRequest;
import com.photoshare.dto.gallery.GalleryResponse;
import com.photoshare.dto.gallery.PinVerifyRequest;
import com.photoshare.dto.gallery.PinVerifyResponse;
import com.photoshare.dto.gallery.PublicGalleryPhotoResponse;
import com.photoshare.entity.Event;
import com.photoshare.entity.Gallery;
import com.photoshare.entity.GalleryAccessToken;
import com.photoshare.entity.Photo;
import com.photoshare.entity.PinAttempt;
import com.photoshare.entity.User;
import com.photoshare.exception.ApiException;
import com.photoshare.repository.EventRepository;
import com.photoshare.repository.GalleryAccessTokenRepository;
import com.photoshare.repository.GalleryRepository;
import com.photoshare.repository.PhotoRepository;
import com.photoshare.repository.PinAttemptRepository;
import com.photoshare.storage.StorageService;
import com.photoshare.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GalleryService {

    private final GalleryRepository galleryRepository;
    private final PhotoRepository photoRepository;
    private final PinAttemptRepository pinAttemptRepository;
    private final GalleryAccessTokenRepository galleryAccessTokenRepository;
    private final EventRepository eventRepository;
    private final PhotoService photoService;
    private final StorageService storageService;
    private final AppProperties appProperties;

    public GalleryResponse createGallery(Event event, GalleryCreateRequest request, User admin, String frontendUrl) {
        if (galleryRepository.existsByEventId(event.getId())) {
            throw ApiException.conflict("Gallery already exists for this event");
        }

        List<Photo> selectedPhotos = photoRepository.findByEventIdAndSelectedForPublishingTrue(event.getId());
        if (selectedPhotos.isEmpty()) {
            throw ApiException.badRequest("No photos selected for gallery");
        }

        String publicToken = SecurityUtil.generateSecureToken(32);
        String pinHash = SecurityUtil.hashPin(request.getPin());

        List<Gallery.GalleryPhotoInfo> photos = new java.util.ArrayList<>();
        for (int i = 0; i < selectedPhotos.size(); i++) {
            Gallery.GalleryPhotoInfo photoInfo = Gallery.GalleryPhotoInfo.builder()
                    .photoId(selectedPhotos.get(i).getId())
                    .displayOrder(i)
                    .build();
            photos.add(photoInfo);
        }

        Gallery gallery = Gallery.builder()
                .id(UUID.randomUUID())
                .eventId(event.getId())
                .publicToken(publicToken)
                .pinHash(pinHash)
                .published(false)
                .photos(photos)
                .build();

        Gallery saved = galleryRepository.save(gallery);

        return GalleryResponse.from(saved, frontendUrl, selectedPhotos.size(), event.getName());
    }

    public GalleryResponse getGallery(Event event, User admin) {
        Gallery gallery = galleryRepository.findByEventId(event.getId())
                .orElseThrow(() -> ApiException.notFound("Gallery not found"));

        int photoCount = gallery.getPhotos() != null ? gallery.getPhotos().size() : 0;
        return GalleryResponse.from(gallery, appProperties.getFrontendUrl(), photoCount, event.getName());
    }

    public GalleryResponse publishGallery(Event event, User admin, String frontendUrl) {
        Gallery gallery = galleryRepository.findByEventId(event.getId())
                .orElseThrow(() -> ApiException.notFound("Gallery not found"));

        if (gallery.getPublished()) {
            throw ApiException.badRequest("Gallery is already published");
        }

        List<Photo> selectedPhotos = photoRepository.findByEventIdAndSelectedForPublishingTrue(event.getId());
        if (selectedPhotos.isEmpty()) {
            throw ApiException.badRequest("No photos selected for gallery");
        }

        // Update the gallery with current selected photos
        List<Gallery.GalleryPhotoInfo> photos = selectedPhotos.stream()
                .map(photo -> Gallery.GalleryPhotoInfo.builder()
                        .photoId(photo.getId())
                        .displayOrder(0)
                        .build())
                .collect(Collectors.toList());

        gallery.setPhotos(photos);
        gallery.setPublished(true);
        gallery.setPublishedAt(Instant.now());
        galleryRepository.save(gallery);

        return GalleryResponse.from(gallery, frontendUrl, selectedPhotos.size(), event.getName());
    }

    public void unpublishGallery(Event event, User admin) {
        Gallery gallery = galleryRepository.findByEventId(event.getId())
                .orElseThrow(() -> ApiException.notFound("Gallery not found"));

        gallery.setPublished(false);
        gallery.setPublishedAt(null);
        galleryRepository.save(gallery);
    }

    public PinVerifyResponse verifyPin(String publicToken, PinVerifyRequest request, String ipAddress) {
        Gallery gallery = galleryRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> ApiException.notFound("Gallery not found"));

        if (!gallery.getPublished()) {
            throw ApiException.notFound("Gallery not found");
        }

        // Get pin attempt for this gallery and IP
        PinAttempt attempt = pinAttemptRepository.findByGalleryIdAndIpAddress(gallery.getId(), ipAddress).orElse(null);
        Instant now = Instant.now();

        if (attempt != null && attempt.getLockedUntil() != null && attempt.getLockedUntil().isAfter(now)) {
            throw ApiException.unauthorized("Too many failed attempts. Please try again later.");
        }

        if (!SecurityUtil.verifyPin(request.getPin(), gallery.getPinHash())) {
            Instant lockoutTime = now.plusSeconds(appProperties.getGallery().getPinLockoutDuration());
            int maxAttempts = appProperties.getGallery().getMaxPinAttempts();
            
            // Handle pin attempt logic
            if (attempt == null) {
                attempt = PinAttempt.builder()
                        .id(UUID.randomUUID())
                        .galleryId(gallery.getId())
                        .ipAddress(ipAddress)
                        .attemptCount(1)
                        .lockedUntil(lockoutTime)
                        .build();
            } else {
                attempt.setAttemptCount(attempt.getAttemptCount() + 1);
                attempt.setLockedUntil(lockoutTime);
            }
            pinAttemptRepository.save(attempt);
            
            throw ApiException.unauthorized("Invalid PIN");
        }

        // Reset attempts on success
        if (attempt != null) {
            pinAttemptRepository.deleteByGalleryIdAndIpAddress(gallery.getId(), ipAddress);
        }

        String accessToken = SecurityUtil.generateSecureToken(64);
        Instant expiresAt = now.plusSeconds(appProperties.getGallery().getAccessTokenExpiration());

        GalleryAccessToken token = GalleryAccessToken.builder()
                .id(UUID.randomUUID())
                .galleryId(gallery.getId())
                .token(accessToken)
                .ipAddress(ipAddress)
                .expiresAt(expiresAt)
                .build();

        galleryAccessTokenRepository.save(token);

        PinVerifyResponse response = new PinVerifyResponse();
        response.setAccessToken(accessToken);
        response.setExpiresIn(appProperties.getGallery().getAccessTokenExpiration());
        response.setGalleryToken(publicToken);
        return response;
    }

    public List<PublicGalleryPhotoResponse> getPublicPhotos(String accessToken) {
        GalleryAccessToken token = galleryAccessTokenRepository.findValidToken(accessToken, Instant.now())
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired access token"));

        Gallery gallery = galleryRepository.findById(token.getGalleryId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired access token"));

        if (gallery.getPhotos() == null || gallery.getPhotos().isEmpty()) {
            return List.of();
        }

        // Get photo IDs from gallery
        List<UUID> photoIds = gallery.getPhotos().stream()
                .map(Gallery.GalleryPhotoInfo::getPhotoId)
                .collect(Collectors.toList());

        // Fetch photos
        List<Photo> photos = photoRepository.findAllById(photoIds);
        
        // Sort by display order
        photos.sort((p1, p2) -> {
            Integer order1 = gallery.getPhotos().stream()
                    .filter(gp -> gp.getPhotoId().equals(p1.getId()))
                    .map(Gallery.GalleryPhotoInfo::getDisplayOrder)
                    .findFirst()
                    .orElse(0);
            Integer order2 = gallery.getPhotos().stream()
                    .filter(gp -> gp.getPhotoId().equals(p2.getId()))
                    .map(Gallery.GalleryPhotoInfo::getDisplayOrder)
                    .findFirst()
                    .orElse(0);
            return order1.compareTo(order2);
        });

        return photos.stream()
                .map(photo -> {
                    String photoPath = photoService.getPhotoPath(photo);
                    return PublicGalleryPhotoResponse.from(photo, photoPath, 0);
                })
                .collect(Collectors.toList());
    }

    public Resource getPhotoResource(String accessToken, UUID photoId) {
        GalleryAccessToken token = galleryAccessTokenRepository.findValidToken(accessToken, Instant.now())
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired access token"));

        Gallery gallery = galleryRepository.findById(token.getGalleryId())
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired access token"));

        if (gallery.getPhotos() == null || gallery.getPhotos().isEmpty()) {
            throw ApiException.notFound("No photos in gallery");
        }

        // Check if photo belongs to this gallery
        boolean photoInGallery = gallery.getPhotos().stream()
                .anyMatch(gp -> gp.getPhotoId().equals(photoId));
        
        if (!photoInGallery) {
            throw ApiException.forbidden("Photo not in this gallery");
        }

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("Photo not found"));

        try {
            return photoService.loadPhotoAsResource(photo);
        } catch (Exception e) {
            throw ApiException.notFound("Photo file not found");
        }
    }

    public GalleryResponse.PublicGalleryInfo getPublicGalleryInfo(String publicToken) {
        Gallery gallery = galleryRepository.findByPublicToken(publicToken)
                .orElseThrow(() -> ApiException.notFound("Gallery not found"));

        if (!gallery.getPublished()) {
            throw ApiException.notFound("Gallery not found");
        }

        Event event = eventRepository.findById(gallery.getEventId())
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        GalleryResponse.PublicGalleryInfo info = new GalleryResponse.PublicGalleryInfo();
        info.setEventName(event.getName());
        info.setEventDate(event.getEventDate());
        info.setGalleryToken(gallery.getPublicToken());
        info.setRequiresPin(true);
        info.setPhotoCount(gallery.getPhotos() != null ? gallery.getPhotos().size() : 0);
        return info;
    }

    public boolean existsByEvent(Event event) {
        return galleryRepository.existsByEventId(event.getId());
    }

    public boolean isPublishedByEvent(Event event) {
        return galleryRepository.findByEventIdAndPublishedTrue(event.getId()).isPresent();
    }

    public void cleanupExpiredTokens() {
        galleryAccessTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
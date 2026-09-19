package com.photoshare.service;

import com.photoshare.config.AppProperties;
import com.photoshare.dto.photo.PhotoResponse;
import com.photoshare.entity.Event;
import com.photoshare.entity.Photo;
import com.photoshare.entity.User;
import com.photoshare.exception.ApiException;
import com.photoshare.repository.EventMemberRepository;
import com.photoshare.repository.PhotoRepository;
import com.photoshare.storage.StorageService;
import com.photoshare.storage.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final EventMemberRepository eventMemberRepository;
    private final StorageService storageService;
    private final AppProperties appProperties;

    public PhotoResponse uploadPhoto(Event event, User user, MultipartFile file) throws IOException {
        storageService.validateFile(file);

        String storageKey = storageService.store(file, event.getId(), file.getOriginalFilename());

        Photo photo = Photo.builder()
                .id(UUID.randomUUID())
                .eventId(event.getId())
                .uploadedBy(user.getId())
                .originalFilename(file.getOriginalFilename())
                .storageKey(storageKey)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .selectedForPublishing(false)
                .build();

        Photo saved = photoRepository.save(photo);

        return PhotoResponse.fromWithSignedUrl(saved, null); // No signed URL for local storage
    }

    public PhotoResponse getPhotoById(UUID photoId, User user) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> ApiException.notFound("Photo not found"));

        if (!photo.getUploadedBy().equals(user.getId())) {
            throw ApiException.forbidden("You can only access your own photos");
        }

        return PhotoResponse.fromWithSignedUrl(photo, null);
    }

    public List<PhotoResponse> getEventPhotos(Event event, User user) {
        return photoRepository.findByEventId(event.getId()).stream()
                .map(photo -> PhotoResponse.fromWithSignedUrl(photo, null))
                .toList();
    }

    public Page<PhotoResponse> getEventPhotosPaged(Event event, User user, Pageable pageable) {
        return photoRepository.findByEventId(event.getId(), pageable)
                .map(photo -> PhotoResponse.fromWithSignedUrl(photo, null));
    }

    public List<PhotoResponse> getMyPhotos(Event event, User user) {
        return photoRepository.findByEventIdAndUploadedBy(event.getId(), user.getId()).stream()
                .map(photo -> PhotoResponse.fromWithSignedUrl(photo, null))
                .toList();
    }

    public Page<PhotoResponse> getMyPhotosPaged(Event event, User user, Pageable pageable) {
        return photoRepository.findByEventIdAndUploadedBy(event.getId(), user.getId(), pageable)
                .map(photo -> PhotoResponse.fromWithSignedUrl(photo, null));
    }

    public List<PhotoResponse> getSelectedPhotos(Event event) {
        return photoRepository.findByEventIdAndSelectedForPublishingTrue(event.getId()).stream()
                .map(PhotoResponse::from)
                .toList();
    }

    public void updateSelection(Event event, List<UUID> photoIds, boolean selected, User admin) {
        List<Photo> photos = photoRepository.findAllById(photoIds);
        for (Photo photo : photos) {
            if (!photo.getEventId().equals(event.getId())) {
                throw ApiException.badRequest("Photo does not belong to this event");
            }
            photo.setSelectedForPublishing(selected);
        }
        photoRepository.saveAll(photos);
    }

    public long countByEvent(Event event) {
        return photoRepository.countByEventId(event.getId());
    }

    public long countSelectedByEvent(Event event) {
        return photoRepository.countByEventIdAndSelectedForPublishingTrue(event.getId());
    }

    public Photo findByIdAndEvent(UUID photoId, Event event) {
        return photoRepository.findByIdAndEventId(photoId, event.getId())
                .orElseThrow(() -> ApiException.notFound("Photo not found"));
    }

    public String getPhotoPath(Photo photo) {
        return storageService.getFilePath(photo.getStorageKey());
    }

    public org.springframework.core.io.Resource loadPhotoAsResource(Photo photo) throws StorageException {
        return storageService.loadAsResource(photo.getStorageKey());
    }

    public void deletePhoto(Photo photo) throws StorageException {
        storageService.delete(photo.getStorageKey());
    }
}
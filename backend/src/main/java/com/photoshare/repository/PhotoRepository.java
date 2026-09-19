package com.photoshare.repository;

import com.photoshare.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PhotoRepository extends MongoRepository<Photo, UUID> {

    List<Photo> findByEventId(UUID eventId);

    Page<Photo> findByEventId(UUID eventId, Pageable pageable);

    List<Photo> findByEventIdAndUploadedBy(UUID eventId, UUID userId);

    List<Photo> findByEventIdAndSelectedForPublishingTrue(UUID eventId);

    long countByEventId(UUID eventId);

    long countByEventIdAndSelectedForPublishingTrue(UUID eventId);

    Page<Photo> findByEventIdAndUploadedBy(UUID eventId, UUID userId, Pageable pageable);

    Optional<Photo> findByIdAndEventId(UUID id, UUID eventId);
}
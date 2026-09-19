package com.photoshare.repository;

import com.photoshare.entity.PinAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PinAttemptRepository extends MongoRepository<PinAttempt, UUID> {

    Optional<PinAttempt> findByGalleryIdAndIpAddress(UUID galleryId, String ipAddress);

    void deleteByGalleryIdAndIpAddress(UUID galleryId, String ipAddress);

    void deleteByLockedUntilBefore(Instant now);
}
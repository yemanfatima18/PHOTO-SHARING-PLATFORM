package com.photoshare.repository;

import com.photoshare.entity.Gallery;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryRepository extends MongoRepository<Gallery, UUID> {

    Optional<Gallery> findByPublicToken(String publicToken);

    Optional<Gallery> findByEventId(UUID eventId);

    Optional<Gallery> findByEventIdAndPublishedTrue(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
package com.photoshare.repository;

import com.photoshare.entity.GalleryAccessToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GalleryAccessTokenRepository extends MongoRepository<GalleryAccessToken, UUID> {

    Optional<GalleryAccessToken> findByToken(String token);

    @Query("{ 'token': ?0, 'expiresAt': { $gt: ?1 } }")
    Optional<GalleryAccessToken> findValidToken(String token, Instant now);

    void deleteByGalleryIdAndExpiresAtBefore(UUID galleryId, Instant now);

    void deleteByExpiresAtBefore(Instant now);
}
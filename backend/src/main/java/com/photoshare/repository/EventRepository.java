package com.photoshare.repository;

import com.photoshare.entity.Event;
import com.photoshare.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends MongoRepository<Event, UUID> {

    List<Event> findByCreatedBy(UUID userId);

    Page<Event> findByCreatedBy(UUID userId, Pageable pageable);

    // For assigned events, we'll query event_members collection first
    // These methods will be implemented in service layer using multiple queries
    // Keeping them for interface compatibility but they'll need custom implementation
    Optional<Event> findByIdAndCreatedBy(UUID id, UUID userId);

    boolean existsByIdAndCreatedBy(UUID id, UUID userId);
}
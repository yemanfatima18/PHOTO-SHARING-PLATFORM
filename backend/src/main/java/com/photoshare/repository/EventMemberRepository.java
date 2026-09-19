package com.photoshare.repository;

import com.photoshare.entity.EventMember;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventMemberRepository extends MongoRepository<EventMember, UUID> {

    Optional<EventMember> findByEventIdAndUserId(UUID eventId, UUID userId);

    boolean existsByEventIdAndUserId(UUID eventId, UUID userId);

    List<EventMember> findByEventId(UUID eventId);

    List<EventMember> findByUserId(UUID userId);

    void deleteByEventIdAndUserId(UUID eventId, UUID userId);
}
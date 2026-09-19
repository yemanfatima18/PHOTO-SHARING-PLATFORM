package com.photoshare.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "pin_attempts")
@CompoundIndex(name = "uk_pin_attempts_gallery_ip", def = "{'galleryId': 1, 'ipAddress': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PinAttempt implements Persistable<UUID> {

    @Id
    private UUID id;

    @Indexed
    private UUID galleryId;

    @Indexed
    private String ipAddress;

    private Integer attemptCount;
    private Instant lockedUntil;

    @LastModifiedDate
    private Instant lastAttemptAt;

    @CreatedDate
    private Instant createdAt;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }
}
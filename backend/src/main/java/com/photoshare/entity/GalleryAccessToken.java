package com.photoshare.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "gallery_access_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GalleryAccessToken implements Persistable<UUID> {

    @Id
    private UUID id;

    @Indexed
    private UUID galleryId;

    @Indexed(unique = true)
    private String token;

    private String ipAddress;
    private String userAgent;

    @Indexed
    private Instant expiresAt;

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
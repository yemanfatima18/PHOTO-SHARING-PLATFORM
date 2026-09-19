package com.photoshare.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "photos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Photo implements Persistable<UUID> {

    @Id
    private UUID id;

    @Indexed
    private UUID eventId;

    @Indexed
    private UUID uploadedBy;

    @Indexed
    private Boolean selectedForPublishing;

    private String originalFilename;
    private String storageKey;
    private Long fileSize;
    private String contentType;

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
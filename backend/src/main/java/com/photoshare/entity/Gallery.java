package com.photoshare.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Document(collection = "galleries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Gallery implements Persistable<UUID> {

    @Id
    private UUID id;

    @Indexed
    private UUID eventId;

    @Indexed(unique = true)
    private String publicToken;

    @Indexed
    private Boolean published;

    private String pinHash;
    private Instant publishedAt;

    @Field("photos")
    @Builder.Default
    private List<GalleryPhotoInfo> photos = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return createdAt == null;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GalleryPhotoInfo {
        private UUID photoId;
        private Integer displayOrder;
    }
}
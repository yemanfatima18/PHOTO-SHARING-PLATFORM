package com.photoshare.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Persistable;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "event_members")
@CompoundIndex(name = "uk_event_members_event_user", def = "{'eventId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMember implements Persistable<UUID> {

    @Id
    private UUID id;

    @Indexed
    private UUID eventId;

    @Indexed
    private UUID userId;

    @CreatedDate
    private Instant assignedAt;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return assignedAt == null;
    }
}
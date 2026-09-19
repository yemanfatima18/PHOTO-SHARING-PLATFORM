package com.photoshare.dto.event;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class EventMemberRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;
}
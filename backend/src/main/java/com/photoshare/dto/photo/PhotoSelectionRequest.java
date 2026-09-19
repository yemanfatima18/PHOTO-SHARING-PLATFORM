package com.photoshare.dto.photo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class PhotoSelectionRequest {

    @NotNull(message = "Photo IDs are required")
    private List<UUID> photoIds;

    @NotNull(message = "Selection status is required")
    private Boolean selected;
}
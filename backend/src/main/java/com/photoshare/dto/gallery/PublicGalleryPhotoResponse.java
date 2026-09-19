package com.photoshare.dto.gallery;

import com.photoshare.entity.Photo;
import lombok.Data;

@Data
public class PublicGalleryPhotoResponse {

    private String id;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private String signedUrl;
    private int displayOrder;

    public static PublicGalleryPhotoResponse from(Photo photo, String signedUrl, int displayOrder) {
        PublicGalleryPhotoResponse response = new PublicGalleryPhotoResponse();
        response.setId(photo.getId().toString());
        response.setOriginalFilename(photo.getOriginalFilename());
        response.setContentType(photo.getContentType());
        response.setFileSize(photo.getFileSize());
        response.setSignedUrl(signedUrl);
        response.setDisplayOrder(displayOrder);
        return response;
    }
}
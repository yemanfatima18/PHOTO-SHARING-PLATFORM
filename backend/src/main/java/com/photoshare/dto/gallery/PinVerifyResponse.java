package com.photoshare.dto.gallery;

import lombok.Data;

@Data
public class PinVerifyResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private String galleryToken;
}
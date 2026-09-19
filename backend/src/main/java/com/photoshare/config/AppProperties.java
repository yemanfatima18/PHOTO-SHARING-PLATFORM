package com.photoshare.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Cors cors = new Cors();
    private Gallery gallery = new Gallery();
    private String frontendUrl;

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
        private long refreshExpiration;
    }

    @Data
    public static class Storage {
        private String path = "./uploads";
        private long maxFileSize = 52428800;
        private String allowedContentTypes = "image/jpeg,image/jpg,image/png,image/webp";
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
    }

    @Data
    public static class Gallery {
        private int pinLength = 6;
        private int accessTokenExpiration = 3600;
        private int maxPinAttempts = 5;
        private int pinLockoutDuration = 900;
    }
}
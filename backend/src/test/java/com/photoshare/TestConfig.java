package com.photoshare;

import com.photoshare.config.AppProperties;
import com.photoshare.security.JwtAuthenticationFilter;
import com.photoshare.security.JwtAuthenticationEntryPoint;
import com.photoshare.security.JwtTokenProvider;
import com.photoshare.security.UserDetailsServiceImpl;
import com.photoshare.storage.LocalStorageService;
import com.photoshare.storage.StorageService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.cors.CorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public AppProperties appProperties() {
        AppProperties props = new AppProperties();
        props.getJwt().setSecret("test-secret-key-for-testing-purposes-only-256-bits-minimum-length");
        props.getJwt().setExpiration(86400000);
        props.getJwt().setRefreshExpiration(604800000);
        try {
            Path tempDir = Files.createTempDirectory("test-uploads");
            tempDir.toFile().deleteOnExit();
            props.getStorage().setPath(tempDir.toString());
        } catch (Exception e) {
            props.getStorage().setPath("./test-uploads");
        }
        props.getStorage().setMaxFileSize(52428800);
        props.getStorage().setAllowedContentTypes("image/jpeg,image/jpg,image/png,image/webp");
        props.getCors().setAllowedOrigins("http://localhost:5173");
        props.getGallery().setPinLength(6);
        props.getGallery().setAccessTokenExpiration(3600);
        props.getGallery().setMaxPinAttempts(5);
        props.getGallery().setPinLockoutDuration(900);
        props.setFrontendUrl("http://localhost:5173");
        return props;
    }

    @Bean
    @Primary
    public StorageService storageService(AppProperties appProperties) {
        return new LocalStorageService(appProperties);
    }

    @Bean
    @Primary
    public JwtTokenProvider jwtTokenProvider(AppProperties appProperties) {
        return new JwtTokenProvider(appProperties);
    }

    @Bean
    @Primary
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserDetailsServiceImpl userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    @Primary
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return new JwtAuthenticationEntryPoint(objectMapper);
    }
}
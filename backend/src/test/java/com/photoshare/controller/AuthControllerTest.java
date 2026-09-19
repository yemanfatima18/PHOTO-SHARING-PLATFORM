package com.photoshare.controller;

import com.photoshare.BaseIntegrationTest;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.entity.User;
import com.photoshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_succeeds() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test Admin");
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        ResultActions result = performPostPublic("/api/auth/register", request);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000))
                .andExpect(jsonPath("$.user.email").value("admin@test.com"))
                .andExpect(jsonPath("$.user.name").value("Test Admin"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void register_duplicateEmail_rejected() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("Test Admin");
        request.setEmail("admin@test.com");
        request.setPassword("password123");

        performPostPublic("/api/auth/register", request).andExpect(status().isCreated());

        ResultActions result = performPostPublic("/api/auth/register", request);

        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    void login_succeeds() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Admin");
        registerRequest.setEmail("admin@test.com");
        registerRequest.setPassword("password123");

        performPostPublic("/api/auth/register", registerRequest).andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword("password123");

        ResultActions result = performPostPublic("/api/auth/login", loginRequest);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(86400000))
                .andExpect(jsonPath("$.user.email").value("admin@test.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void login_invalidPassword_rejected() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Admin");
        registerRequest.setEmail("admin@test.com");
        registerRequest.setPassword("password123");

        performPostPublic("/api/auth/register", registerRequest).andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("admin@test.com");
        loginRequest.setPassword("wrongpassword");

        ResultActions result = performPostPublic("/api/auth/login", loginRequest);

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonexistentUser_rejected() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("nonexistent@test.com");
        loginRequest.setPassword("password123");

        ResultActions result = performPostPublic("/api/auth/login", loginRequest);

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsCurrentUser() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setName("Test Admin");
        registerRequest.setEmail("admin@test.com");
        registerRequest.setPassword("password123");

        ResultActions registerResult = performPostPublic("/api/auth/register", registerRequest);
        String token = extractToken(registerResult);

        ResultActions result = performGet("/api/auth/me", token);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.name").value("Test Admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
}
package com.photoshare;

import com.photoshare.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User adminUser;
    protected User teamMemberUser;
    protected String adminToken;
    protected String teamMemberToken;

    @BeforeEach
    void setUp() {
        // Will be overridden in subclasses
    }

    protected String toJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected ResultActions performPost(String url, Object body, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions performGet(String url, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions performPatch(String url, Object body, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body))
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions performDelete(String url, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(url)
                .header("Authorization", "Bearer " + token));
    }

    protected ResultActions performPostPublic(String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(body)));
    }

    protected ResultActions performGetPublic(String url) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url));
    }

    protected ResultActions performGetPublicWithToken(String url, String accessToken) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", "Bearer " + accessToken));
    }

    protected String extractToken(ResultActions result) throws Exception {
        String response = result.andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    protected User createTestAdmin(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test Admin")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();
    }

    protected User createTestTeamMember(String email) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Test Member")
                .email(email)
                .passwordHash(passwordEncoder.encode("password123"))
                .role(User.Role.TEAM_MEMBER)
                .enabled(true)
                .build();
    }
}
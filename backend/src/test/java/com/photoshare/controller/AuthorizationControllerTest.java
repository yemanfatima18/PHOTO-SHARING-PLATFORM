package com.photoshare.controller;

import com.photoshare.BaseIntegrationTest;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.dto.event.EventRequest;
import com.photoshare.entity.Event;
import com.photoshare.entity.EventMember;
import com.photoshare.entity.User;
import com.photoshare.repository.EventMemberRepository;
import com.photoshare.repository.EventRepository;
import com.photoshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthorizationControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMemberRepository eventMemberRepository;

    private String adminToken;
    private String teamMemberToken;
    private UUID adminId;
    private UUID teamMemberId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        eventRepository.deleteAll();
        eventMemberRepository.deleteAll();

        // Create admin user
        RegisterRequest adminRegister = new RegisterRequest();
        adminRegister.setName("Admin User");
        adminRegister.setEmail("admin@test.com");
        adminRegister.setPassword("password123");
        ResultActions adminRegResult = performPostPublic("/api/auth/register", adminRegister);
        adminToken = extractToken(adminRegResult);
        adminId = UUID.fromString(objectMapper.readTree(adminRegResult.andReturn().getResponse().getContentAsString()).get("user").get("id").asText());

        // Create team member user
        RegisterRequest memberRegister = new RegisterRequest();
        memberRegister.setName("Team Member");
        memberRegister.setEmail("member@test.com");
        memberRegister.setPassword("password123");
        ResultActions memberRegResult = performPostPublic("/api/auth/register", memberRegister);
        teamMemberToken = extractToken(memberRegResult);
        teamMemberId = UUID.fromString(objectMapper.readTree(memberRegResult.andReturn().getResponse().getContentAsString()).get("user").get("id").asText());

        // Manually set role to TEAM_MEMBER for member
        User member = userRepository.findByEmail("member@test.com").orElseThrow();
        member.setRole(User.Role.TEAM_MEMBER);
        userRepository.save(member);

        // Re-login to get updated token with TEAM_MEMBER role
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("member@test.com");
        loginRequest.setPassword("password123");
        ResultActions loginResult = performPostPublic("/api/auth/login", loginRequest);
        teamMemberToken = extractToken(loginResult);
    }

    private EventRequest createEventRequest() {
        EventRequest request = new EventRequest();
        request.setName("Test Event");
        request.setDescription("Test Description");
        request.setEventDate(LocalDate.now().plusDays(1));
        return request;
    }

    @Test
    void admin_canCreateEvent() throws Exception {
        EventRequest request = createEventRequest();

        ResultActions result = performPost("/api/events", request, adminToken);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Event"))
                .andExpect(jsonPath("$.createdById").value(adminId.toString()));
    }

    @Test
    void teamMember_cannotCreateEvent() throws Exception {
        EventRequest request = createEventRequest();

        ResultActions result = performPost("/api/events", request, teamMemberToken);

        result.andExpect(status().isForbidden());
    }

    @Test
    void teamMember_cannotPublishGallery() throws Exception {
        // Create event as admin
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Try to publish gallery as team member
        ResultActions result = performPost("/api/events/" + eventId + "/gallery/publish", null, teamMemberToken);

        result.andExpect(status().isForbidden());
    }

    @Test
    void teamMember_cannotAccessUnassignedEvent() throws Exception {
        // Create event as admin
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Try to access event as team member (not assigned)
        ResultActions result = performGet("/api/events/" + eventId, teamMemberToken);

        result.andExpect(status().isForbidden());
    }

    @Test
    void unauthorizedRequest_rejected() throws Exception {
        EventRequest request = createEventRequest();

        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request)));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void invalidToken_rejected() throws Exception {
        EventRequest request = createEventRequest();

        ResultActions result = mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(toJson(request))
                .header("Authorization", "Bearer invalid-token"));

        result.andExpect(status().isUnauthorized());
    }

    @Test
    void teamMember_canAccessAssignedEvent() throws Exception {
        // Create event as admin
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Assign team member to event
        EventMember eventMember = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(teamMemberId)
                .build();
        eventMemberRepository.save(eventMember);

        // Try to access event as assigned team member
        ResultActions result = performGet("/api/events/" + eventId, teamMemberToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()));
    }
}
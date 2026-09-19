package com.photoshare.controller;

import com.photoshare.BaseIntegrationTest;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.dto.event.EventMemberRequest;
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
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EventControllerTest extends BaseIntegrationTest {

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
                .andExpect(jsonPath("$.description").value("Test Description"))
                .andExpect(jsonPath("$.createdById").value(adminId.toString()))
                .andExpect(jsonPath("$.photoCount").value(0))
                .andExpect(jsonPath("$.selectedPhotoCount").value(0))
                .andExpect(jsonPath("$.hasGallery").value(false))
                .andExpect(jsonPath("$.galleryPublished").value(false));
    }

    @Test
    void admin_canAssignTeamMember() throws Exception {
        // Create event
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Add team member
        EventMemberRequest memberRequest = new EventMemberRequest();
        memberRequest.setUserId(teamMemberId);

        ResultActions result = performPost("/api/events/" + eventId + "/members", memberRequest, adminToken);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(teamMemberId.toString()))
                .andExpect(jsonPath("$.userEmail").value("member@test.com"))
                .andExpect(jsonPath("$.userRole").value("TEAM_MEMBER"));
    }

    @Test
    void assignedTeamMember_canAccessEvent() throws Exception {
        // Create event
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Assign team member
        EventMember eventMember = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(teamMemberId)
                .build();
        eventMemberRepository.save(eventMember);

        // Access event as team member
        ResultActions result = performGet("/api/events/" + eventId, teamMemberToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.name").value("Test Event"));
    }

    @Test
    void unassignedTeamMember_cannotAccessEvent() throws Exception {
        // Create event
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Try to access event as team member (not assigned)
        ResultActions result = performGet("/api/events/" + eventId, teamMemberToken);

        result.andExpect(status().isForbidden());
    }

    @Test
    void admin_canListMyEvents() throws Exception {
        // Create multiple events
        for (int i = 1; i <= 3; i++) {
            EventRequest request = createEventRequest();
            request.setName("Event " + i);
            request.setDescription("Description " + i);
            request.setEventDate(LocalDate.now().plusDays(i));
            performPost("/api/events", request, adminToken);
        }

        ResultActions result = performGet("/api/events", adminToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void teamMember_canListAssignedEvents() throws Exception {
        // Create event
        EventRequest eventRequest = createEventRequest();

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        UUID eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Assign team member
        EventMember eventMember = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(teamMemberId)
                .build();
        eventMemberRepository.save(eventMember);

        // List events as team member
        ResultActions result = performGet("/api/events", teamMemberToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(eventId.toString()));
    }
}
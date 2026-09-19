package com.photoshare.controller;

import com.photoshare.BaseIntegrationTest;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.dto.event.EventRequest;
import com.photoshare.dto.photo.PhotoSelectionRequest;
import com.photoshare.entity.Event;
import com.photoshare.entity.EventMember;
import com.photoshare.entity.Photo;
import com.photoshare.entity.User;
import com.photoshare.repository.EventMemberRepository;
import com.photoshare.repository.EventRepository;
import com.photoshare.repository.PhotoRepository;
import com.photoshare.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PhotoControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMemberRepository eventMemberRepository;

    @Autowired
    private PhotoRepository photoRepository;

    private String adminToken;
    private String teamMemberToken;
    private UUID adminId;
    private UUID teamMemberId;
    private UUID eventId;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        eventRepository.deleteAll();
        eventMemberRepository.deleteAll();
        photoRepository.deleteAll();

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

        // Create event
        EventRequest eventRequest = new EventRequest();
        eventRequest.setName("Test Event");
        eventRequest.setDescription("Test Description");
        eventRequest.setEventDate(LocalDate.now().plusDays(1));

        ResultActions eventResult = performPost("/api/events", eventRequest, adminToken);
        eventId = UUID.fromString(objectMapper.readTree(eventResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Assign team member
        EventMember eventMember = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(teamMemberId)
                .build();
        eventMemberRepository.save(eventMember);
    }

    @Test
    void authorizedTeamMember_canUploadPhoto() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ResultActions result = mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + teamMemberToken));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.uploadedById").value(teamMemberId.toString()))
                .andExpect(jsonPath("$.originalFilename").value("test.jpg"))
                .andExpect(jsonPath("$.selectedForPublishing").value(false));
    }

    @Test
    void unauthorizedTeamMember_cannotUploadToAnotherEvent() throws Exception {
        // Create another event not assigned to team member
        EventRequest eventRequest2 = new EventRequest();
        eventRequest2.setName("Another Event");
        eventRequest2.setDescription("Another Description");
        eventRequest2.setEventDate(LocalDate.now().plusDays(1));

        ResultActions eventResult2 = performPost("/api/events", eventRequest2, adminToken);
        UUID eventId2 = UUID.fromString(objectMapper.readTree(eventResult2.andReturn().getResponse().getContentAsString()).get("id").asText());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ResultActions result = mockMvc.perform(multipart("/api/events/" + eventId2 + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + teamMemberToken));

        result.andExpect(status().isForbidden());
    }

    @Test
    void admin_canViewEventPhotos() throws Exception {
        // Upload a photo as team member first
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + teamMemberToken));

        // Admin views all photos
        ResultActions result = performGet("/api/events/" + eventId + "/photos", adminToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("test.jpg"));
    }

    @Test
    void teamMember_onlySeesOwnPhotos() throws Exception {
        // Create another team member
        RegisterRequest member2Register = new RegisterRequest();
        member2Register.setName("Team Member 2");
        member2Register.setEmail("member2@test.com");
        member2Register.setPassword("password123");
        ResultActions member2RegResult = performPostPublic("/api/auth/register", member2Register);
        String member2Token = extractToken(member2RegResult);
        UUID member2Id = UUID.fromString(objectMapper.readTree(member2RegResult.andReturn().getResponse().getContentAsString()).get("user").get("id").asText());

        User member2 = userRepository.findByEmail("member2@test.com").orElseThrow();
        member2.setRole(User.Role.TEAM_MEMBER);
        userRepository.save(member2);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("member2@test.com");
        loginRequest.setPassword("password123");
        ResultActions loginResult = performPostPublic("/api/auth/login", loginRequest);
        member2Token = extractToken(loginResult);

        // Assign member2 to event
        EventMember eventMember2 = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(member2Id)
                .build();
        eventMemberRepository.save(eventMember2);

        // Member 1 uploads a photo
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "photo1.jpg",
                "image/jpeg",
                "photo 1 content".getBytes()
        );
        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                .file(file1)
                .header("Authorization", "Bearer " + teamMemberToken));

        // Member 2 uploads a photo
        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "photo2.jpg",
                "image/jpeg",
                "photo 2 content".getBytes()
        );
        mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                .file(file2)
                .header("Authorization", "Bearer " + member2Token));

        // Member 1 sees only their photo
        ResultActions result1 = performGet("/api/events/" + eventId + "/photos/mine", teamMemberToken);
        result1.andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("photo1.jpg"));

        // Member 2 sees only their photo
        ResultActions result2 = performGet("/api/events/" + eventId + "/photos/mine", member2Token);
        result2.andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].originalFilename").value("photo2.jpg"));
    }

    @Test
    void admin_canSelectPhotos() throws Exception {
        // Upload a photo
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        ResultActions uploadResult = mockMvc.perform(multipart("/api/events/" + eventId + "/photos")
                .file(file)
                .header("Authorization", "Bearer " + teamMemberToken));

        UUID photoId = UUID.fromString(objectMapper.readTree(uploadResult.andReturn().getResponse().getContentAsString()).get("id").asText());

        // Admin selects photo for publishing
        PhotoSelectionRequest selectionRequest = new PhotoSelectionRequest();
        selectionRequest.setPhotoIds(List.of(photoId));
        selectionRequest.setSelected(true);

        ResultActions result = performPatch("/api/events/" + eventId + "/photos/selection", selectionRequest, adminToken);

        result.andExpect(status().isOk());

        // Verify photo is selected
        ResultActions photosResult = performGet("/api/events/" + eventId + "/photos/selected", adminToken);
        photosResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].selectedForPublishing").value(true));
    }
}
package com.photoshare.controller;

import com.photoshare.BaseIntegrationTest;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.dto.event.EventRequest;
import com.photoshare.dto.gallery.GalleryCreateRequest;
import com.photoshare.dto.gallery.PinVerifyRequest;
import com.photoshare.dto.gallery.PinVerifyResponse;
import com.photoshare.dto.photo.PhotoSelectionRequest;
import com.photoshare.entity.Event;
import com.photoshare.entity.EventMember;
import com.photoshare.entity.Photo;
import com.photoshare.entity.User;
import com.photoshare.repository.EventMemberRepository;
import com.photoshare.repository.EventRepository;
import com.photoshare.repository.GalleryRepository;
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

class GalleryControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventMemberRepository eventMemberRepository;

    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private GalleryRepository galleryRepository;

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
        galleryRepository.deleteAll();

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

        // Upload a photo and select it
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

        PhotoSelectionRequest selectionRequest = new PhotoSelectionRequest();
        selectionRequest.setPhotoIds(List.of(photoId));
        selectionRequest.setSelected(true);
        performPatch("/api/events/" + eventId + "/photos/selection", selectionRequest, adminToken);
    }

    @Test
    void admin_canCreateGallery() throws Exception {
        GalleryCreateRequest request = new GalleryCreateRequest();
        request.setPin("123456");

        ResultActions result = performPost("/api/events/" + eventId + "/gallery", request, adminToken);

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.publicToken").exists())
                .andExpect(jsonPath("$.published").value(false))
                .andExpect(jsonPath("$.photoCount").value(1))
                .andExpect(jsonPath("$.shareUrl").exists());
    }

    @Test
    void admin_canSelectAndUnselectPhotos() throws Exception {
        // Gallery already has a selected photo from setup
        // Unselect it
        PhotoSelectionRequest unselectRequest = new PhotoSelectionRequest();
        unselectRequest.setPhotoIds(photoRepository.findByEventId(eventId).stream()
                .map(Photo::getId)
                .toList());
        unselectRequest.setSelected(false);
        performPatch("/api/events/" + eventId + "/photos/selection", unselectRequest, adminToken);

        // Try to create gallery without selected photos - should fail
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        galleryResult.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No photos selected for gallery"));

        // Re-select the photo
        PhotoSelectionRequest selectRequest = new PhotoSelectionRequest();
        selectRequest.setPhotoIds(photoRepository.findByEventId(eventId).stream()
                .map(Photo::getId)
                .toList());
        selectRequest.setSelected(true);
        performPatch("/api/events/" + eventId + "/photos/selection", selectRequest, adminToken);

        // Now gallery creation should succeed
        galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        galleryResult.andExpect(status().isCreated());
    }

    @Test
    void admin_canPublishGallery() throws Exception {
        // Create gallery
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        String publicToken = objectMapper.readTree(galleryResult.andReturn().getResponse().getContentAsString()).get("publicToken").asText();

        // Publish gallery
        ResultActions result = performPost("/api/events/" + eventId + "/gallery/publish", null, adminToken);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.publishedAt").exists());
    }

    @Test
    void teamMember_cannotPublishGallery() throws Exception {
        // Create gallery as admin
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);

        // Try to publish as team member
        ResultActions result = performPost("/api/events/" + eventId + "/gallery/publish", null, teamMemberToken);

        result.andExpect(status().isForbidden());
    }

    @Test
    void unpublishedGallery_cannotBeAccessedPublicly() throws Exception {
        // Create gallery but don't publish
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        String publicToken = objectMapper.readTree(galleryResult.andReturn().getResponse().getContentAsString()).get("publicToken").asText();

        // Try to access public info
        ResultActions result = performGetPublic("/api/public/galleries/" + publicToken);

        result.andExpect(status().isNotFound());
    }

    @Test
    void unselectedPhotos_notExposedInPublishedGallery() throws Exception {
        // Create gallery (only selected photos included)
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        String publicToken = objectMapper.readTree(galleryResult.andReturn().getResponse().getContentAsString()).get("publicToken").asText();

        // Publish gallery
        performPost("/api/events/" + eventId + "/gallery/publish", null, adminToken);

        // Verify PIN
        PinVerifyRequest pinRequest = new PinVerifyRequest();
        pinRequest.setPin("123456");
        ResultActions pinResult = performPostPublic("/api/public/galleries/" + publicToken + "/verify", pinRequest);
        String accessToken = objectMapper.readTree(pinResult.andReturn().getResponse().getContentAsString()).get("accessToken").asText();

        // Get photos - should only have the selected one from setup
        ResultActions photosResult = performGetPublicWithToken("/api/public/galleries/" + publicToken + "/photos", accessToken);
        photosResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void pinVerification_incorrectPin_rejected() throws Exception {
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        String publicToken = objectMapper.readTree(galleryResult.andReturn().getResponse().getContentAsString()).get("publicToken").asText();

        performPost("/api/events/" + eventId + "/gallery/publish", null, adminToken);

        PinVerifyRequest pinRequest = new PinVerifyRequest();
        pinRequest.setPin("654321"); // Wrong PIN

        ResultActions result = performPostPublic("/api/public/galleries/" + publicToken + "/verify", pinRequest);

        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid PIN"));
    }

    @Test
    void pinVerification_correctPin_accepted() throws Exception {
        GalleryCreateRequest galleryRequest = new GalleryCreateRequest();
        galleryRequest.setPin("123456");
        ResultActions galleryResult = performPost("/api/events/" + eventId + "/gallery", galleryRequest, adminToken);
        String publicToken = objectMapper.readTree(galleryResult.andReturn().getResponse().getContentAsString()).get("publicToken").asText();

        performPost("/api/events/" + eventId + "/gallery/publish", null, adminToken);

        PinVerifyRequest pinRequest = new PinVerifyRequest();
        pinRequest.setPin("123456"); // Correct PIN

        ResultActions result = performPostPublic("/api/public/galleries/" + publicToken + "/verify", pinRequest);

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.galleryToken").value(publicToken));
    }
}
package com.photoshare.controller;

import com.photoshare.dto.common.PageResponse;
import com.photoshare.dto.event.EventMemberRequest;
import com.photoshare.dto.event.EventMemberResponse;
import com.photoshare.dto.event.EventRequest;
import com.photoshare.dto.event.EventResponse;
import com.photoshare.entity.Event;
import com.photoshare.entity.User;
import com.photoshare.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest request, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        EventResponse response = eventService.create(request, user);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<EventResponse>> getMyEvents(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        User user = (User) authentication.getPrincipal();
        Page<EventResponse> events = eventService.getMyEvents(user, pageable);
        return ResponseEntity.ok(PageResponse.from(events));
    }

    @GetMapping("/list")
    public ResponseEntity<List<EventResponse>> getMyEventsList(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<EventResponse> events = eventService.getMyEventsList(user);
        return ResponseEntity.ok(events);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID eventId, Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        EventResponse response = eventService.getById(eventId, user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{eventId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventMemberResponse> addMember(
            @PathVariable UUID eventId,
            @Valid @RequestBody EventMemberRequest request,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        EventMemberResponse response = eventService.addMember(eventId, request, user);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{eventId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventMemberResponse>> getMembers(
            @PathVariable UUID eventId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<EventMemberResponse> response = eventService.getMembers(eventId, user);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{eventId}/members/{memberId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID eventId,
            @PathVariable UUID memberId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        eventService.removeMember(eventId, memberId, user);
        return ResponseEntity.noContent().build();
    }
}
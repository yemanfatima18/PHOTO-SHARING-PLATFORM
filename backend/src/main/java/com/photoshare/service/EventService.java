package com.photoshare.service;

import com.photoshare.dto.event.EventRequest;
import com.photoshare.dto.event.EventResponse;
import com.photoshare.dto.event.EventMemberRequest;
import com.photoshare.dto.event.EventMemberResponse;
import com.photoshare.entity.Event;
import com.photoshare.entity.EventMember;
import com.photoshare.entity.User;
import com.photoshare.exception.ApiException;
import com.photoshare.repository.EventMemberRepository;
import com.photoshare.repository.EventRepository;
import com.photoshare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventMemberRepository eventMemberRepository;
    private final UserRepository userRepository;
    private final PhotoService photoService;
    private final GalleryService galleryService;

    public EventResponse create(EventRequest request, User admin) {
        Event event = Event.builder()
                .id(UUID.randomUUID())
                .name(request.getName())
                .description(request.getDescription())
                .eventDate(request.getEventDate().atStartOfDay().toInstant(java.time.ZoneOffset.UTC))
                .createdBy(admin.getId())
                .build();

        Event saved = eventRepository.save(event);
        return EventResponse.from(saved, userRepository);
    }

    public EventResponse getById(UUID id, User user) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(user.getId()) &&
            !eventMemberRepository.existsByEventIdAndUserId(id, user.getId())) {
            throw ApiException.forbidden("You do not have access to this event");
        }

        return buildResponse(event);
    }

    public Page<EventResponse> getMyEvents(User user, Pageable pageable) {
        if (user.getRole() == User.Role.ADMIN) {
            return eventRepository.findByCreatedBy(user.getId(), pageable)
                    .map(this::buildResponse);
        } else {
            // For team members, get event IDs from event_members collection
            List<EventMember> memberships = eventMemberRepository.findByUserId(user.getId());
            List<UUID> eventIds = memberships.stream()
                    .map(EventMember::getEventId)
                    .collect(Collectors.toList());
            
            if (eventIds.isEmpty()) {
                return Page.empty(pageable);
            }
            
            // Use a custom query or fetch all and filter
            // For simplicity, we'll fetch all events and filter
            // In production, you'd want a custom repository method
            List<Event> events = eventRepository.findAllById(eventIds);
            return new org.springframework.data.domain.PageImpl<>(
                    events.stream().map(this::buildResponse).collect(Collectors.toList()),
                    pageable,
                    events.size()
            );
        }
    }

    public List<EventResponse> getMyEventsList(User user) {
        if (user.getRole() == User.Role.ADMIN) {
            return eventRepository.findByCreatedBy(user.getId()).stream()
                    .map(this::buildResponse)
                    .collect(Collectors.toList());
        } else {
            List<EventMember> memberships = eventMemberRepository.findByUserId(user.getId());
            List<UUID> eventIds = memberships.stream()
                    .map(EventMember::getEventId)
                    .collect(Collectors.toList());
            
            if (eventIds.isEmpty()) {
                return List.of();
            }
            
            return eventRepository.findAllById(eventIds).stream()
                    .map(this::buildResponse)
                    .collect(Collectors.toList());
        }
    }

    public EventMemberResponse addMember(UUID eventId, EventMemberRequest request, User admin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(admin.getId())) {
            throw ApiException.forbidden("Only the event creator can add members");
        }

        User member = userRepository.findById(request.getUserId())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (member.getRole() != User.Role.TEAM_MEMBER) {
            throw ApiException.badRequest("Only team members can be added to events");
        }

        if (eventMemberRepository.existsByEventIdAndUserId(eventId, member.getId())) {
            throw ApiException.conflict("User is already a member of this event");
        }

        EventMember eventMember = EventMember.builder()
                .id(UUID.randomUUID())
                .eventId(eventId)
                .userId(member.getId())
                .build();

        return EventMemberResponse.from(eventMemberRepository.save(eventMember), member);
    }

    public List<EventMemberResponse> getMembers(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(user.getId())) {
            throw ApiException.forbidden("Only the event creator can view members");
        }

        List<EventMember> members = eventMemberRepository.findByEventId(eventId);
        List<UUID> userIds = members.stream()
                .map(EventMember::getUserId)
                .collect(Collectors.toList());
        
        List<User> users = userRepository.findAllById(userIds);
        
        return members.stream()
                .map(member -> {
                    User u = users.stream()
                            .filter(user1 -> user1.getId().equals(member.getUserId()))
                            .findFirst()
                            .orElse(null);
                    return EventMemberResponse.from(member, u);
                })
                .collect(Collectors.toList());
    }

    public void removeMember(UUID eventId, UUID memberId, User admin) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(admin.getId())) {
            throw ApiException.forbidden("Only the event creator can remove members");
        }

        User member = userRepository.findById(memberId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        eventMemberRepository.deleteByEventIdAndUserId(eventId, memberId);
    }

    public void verifyAdminAccess(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(user.getId())) {
            throw ApiException.forbidden("You do not have permission to manage this event");
        }
    }

    public void verifyMemberAccess(UUID eventId, User user) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> ApiException.notFound("Event not found"));

        if (!event.getCreatedBy().equals(user.getId()) &&
            !eventMemberRepository.existsByEventIdAndUserId(eventId, user.getId())) {
            throw ApiException.forbidden("You do not have access to this event");
        }
    }

    private EventResponse buildResponse(Event event) {
        long photoCount = photoService.countByEvent(event);
        long selectedCount = photoService.countSelectedByEvent(event);
        boolean hasGallery = galleryService.existsByEvent(event);
        boolean galleryPublished = galleryService.isPublishedByEvent(event);

        return EventResponse.from(event, photoCount, selectedCount, hasGallery, galleryPublished, userRepository);
    }
}
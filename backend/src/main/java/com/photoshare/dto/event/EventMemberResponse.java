package com.photoshare.dto.event;

import com.photoshare.entity.EventMember;
import com.photoshare.entity.User;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class EventMemberResponse {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private User.Role userRole;
    private Instant assignedAt;

    public static EventMemberResponse from(EventMember eventMember, User user) {
        EventMemberResponse response = new EventMemberResponse();
        response.setId(eventMember.getId().toString());
        response.setUserId(eventMember.getUserId().toString());
        if (user != null) {
            response.setUserName(user.getName());
            response.setUserEmail(user.getEmail());
            response.setUserRole(user.getRole());
        }
        response.setAssignedAt(eventMember.getAssignedAt());
        return response;
    }
}
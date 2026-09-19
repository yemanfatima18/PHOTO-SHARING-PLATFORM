package com.photoshare.dto.auth;

import com.photoshare.entity.User;
import lombok.Data;

@Data
public class AuthResponse {

    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDto user;

    public static AuthResponse from(String accessToken, long expiresIn, User user) {
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setExpiresIn(expiresIn);
        response.setUser(new UserDto(user));
        return response;
    }

    @Data
    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private User.Role role;

        public UserDto(User user) {
            this.id = user.getId().toString();
            this.name = user.getName();
            this.email = user.getEmail();
            this.role = user.getRole();
        }
    }
}
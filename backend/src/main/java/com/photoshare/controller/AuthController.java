package com.photoshare.controller;

import com.photoshare.dto.auth.AuthResponse;
import com.photoshare.dto.auth.LoginRequest;
import com.photoshare.dto.auth.RegisterRequest;
import com.photoshare.entity.User;
import com.photoshare.security.JwtTokenProvider;
import com.photoshare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());
        return ResponseEntity.status(201).body(AuthResponse.from(token, 86400000, user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.createToken(user.getEmail(), user.getId(), user.getRole().name());

        return ResponseEntity.ok(AuthResponse.from(token, 86400000, user));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> getCurrentUser(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(new AuthResponse.UserDto(user));
    }
}
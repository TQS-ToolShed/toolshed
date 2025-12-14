package com.toolshed.backend.service;

import com.toolshed.backend.dto.LoginRequest;
import com.toolshed.backend.dto.LoginResponse;
import com.toolshed.backend.dto.RegisterRequest;
import com.toolshed.backend.repository.UserRepository;
import com.toolshed.backend.repository.entities.User;
import com.toolshed.backend.repository.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public User register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword()) // In real app, encode this!
                .role(request.getRole())
                .status(UserStatus.ACTIVE) // Default status
                .reputationScore(0.0)
                .build();

        return userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // In a real app, use passwordEncoder.matches(request.getPassword(),
        // user.getPassword())
        if (!user.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // Check account status with specific messages
        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is suspended");
        }
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not verified");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
        }

        // Generate a mock token (UUID) for now since we don't have JWT set up
        String token = UUID.randomUUID().toString();

        return LoginResponse.builder()
                .token(token)
                .user(user)
                .build();
    }

    public boolean isEmailTaken(String email) {
        return userRepository.existsByEmail(email);
    }
}

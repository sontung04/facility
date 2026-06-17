package com.example.facility.identity.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.identity.dto.request.UserUpdateRequest;
import com.example.facility.identity.dto.response.UserResponse;
import com.example.facility.identity.model.User;
import com.example.facility.identity.model.UserRole;
import com.example.facility.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<UserResponse> getUsers(String role) {
        List<User> users;
        if (role != null && !role.isBlank()) {
            UserRole userRole = parseRole(role);
            users = userRepository.findByRole(userRole);
        } else {
            // Exclude the deprecated USER role from listings
            users = userRepository.findAll().stream()
                    .filter(u -> u.getRole() != UserRole.USER)
                    .toList();
        }
        return users.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return mapToResponse(userRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND)));
    }

    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        if (request.getRole() != null) {
            user.setRole(parseRole(request.getRole()));
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        return mapToResponse(userRepository.save(user));
    }

    private UserRole parseRole(String role) {
        try {
            UserRole parsed = UserRole.valueOf(role.toUpperCase());
            if (parsed == UserRole.USER) throw new WebException(ErrorCode.INVALID_REQUEST);
            return parsed;
        } catch (IllegalArgumentException e) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }
    }

    UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .enabled(user.getEnabled())
                .authType(user.getAuthType() != null ? user.getAuthType().name() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}


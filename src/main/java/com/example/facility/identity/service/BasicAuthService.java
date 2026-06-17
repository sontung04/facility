package com.example.facility.identity.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.identity.model.AuthType;
import com.example.facility.identity.model.User;
import com.example.facility.identity.model.UserRole;
import com.example.facility.identity.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BasicAuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public BasicAuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Register a new user via basic auth. Only admins can call this.
     * @param role optional role (MANAGER or TECHNICIAN); defaults to MANAGER; ADMIN is forbidden
     */
    public User registerBasicAuth(String username, String email, String password, String role, String adminUsername) {
        log.info("Registering new user via basic auth: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new WebException(ErrorCode.USERNAME_EXISTS);
        }

        if (userRepository.existsByEmail(email)) {
            throw new WebException(ErrorCode.EMAIL_EXISTS);
        }

        User admin = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new WebException(ErrorCode.USER_NOT_FOUND));

        if (!admin.getRole().equals(UserRole.ADMIN)) {
            throw new WebException(ErrorCode.ADMIN_ONLY_OPERATION);
        }

        UserRole assignedRole = UserRole.MANAGER;
        if (role != null && !role.isBlank()) {
            try {
                assignedRole = UserRole.valueOf(role.toUpperCase());
                if (assignedRole == UserRole.ADMIN) {
                    throw new WebException(ErrorCode.ADMIN_ONLY_OPERATION);
                }
            } catch (IllegalArgumentException e) {
                throw new WebException(ErrorCode.INVALID_REQUEST);
            }
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(passwordEncoder.encode(password));
        newUser.setRole(assignedRole);
        newUser.setEnabled(true);
        newUser.setAuthType(AuthType.BASIC_AUTH);

        log.info("Creating new basic auth user: {} with role: {}", username, assignedRole);
        return userRepository.save(newUser);
    }

    public User authenticateBasicAuth(String username, String password) {
        log.info("Authenticating user via basic auth: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new WebException(ErrorCode.INVALID_USERNAME_PASSWORD));

        if (!user.getEnabled()) {
            throw new WebException(ErrorCode.USER_ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new WebException(ErrorCode.INVALID_USERNAME_PASSWORD);
        }

        if (!user.getAuthType().equals(AuthType.BASIC_AUTH)) {
            throw new WebException(ErrorCode.USER_NOT_REGISTERED_FOR_AUTH);
        }

        log.info("User authenticated successfully: {}", username);
        return user;
    }

    public User createInitialAdmin(String username, String email, String password) {
        log.info("Creating initial admin user: {}", username);

        if (userRepository.existsByUsername(username)) {
            throw new WebException(ErrorCode.USERNAME_EXISTS);
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        admin.setAuthType(AuthType.BASIC_AUTH);

        return userRepository.save(admin);
    }
}


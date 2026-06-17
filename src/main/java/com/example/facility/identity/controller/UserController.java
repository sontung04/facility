package com.example.facility.identity.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.identity.dto.request.UserUpdateRequest;
import com.example.facility.identity.dto.response.UserResponse;
import com.example.facility.identity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // List all users — optional ?role= filter for admin tooling and dispatch UI
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getUsers(
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully",
                userService.getUsers(role)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully",
                userService.getUserById(id)));
    }

    // Update role or enabled status (e.g. disable a user account)
    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id, @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User updated successfully",
                userService.updateUser(id, request)));
    }
}


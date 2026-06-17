package com.example.facility.identity.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.identity.dto.request.LoginRequest;
import com.example.facility.identity.dto.request.RefreshTokenRequest;
import com.example.facility.identity.dto.request.RegisterRequest;
import com.example.facility.identity.dto.response.AuthResponse;
import com.example.facility.identity.service.AuthenticationService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@RestController
@RequestMapping("/auth/v1")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Running endpoint /auth/v1/register with auth type: {}", request.getAuthType());
        return new ApiResponse<>(authenticationService.register(request));
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Running endpoint /auth/v1/login");
        return new ApiResponse<>(authenticationService.login(request));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Running endpoint /auth/v1/refresh");
        return new ApiResponse<>(authenticationService.refresh(request));
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/logout")
    public void logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        log.info("Running endpoint /auth/v1/logout");
        authenticationService.logout(authorizationHeader);
    }
}


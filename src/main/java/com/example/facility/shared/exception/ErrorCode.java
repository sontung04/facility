package com.example.facility.shared.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // General
    INVALID_ARGUMENTS(1004, "Invalid arguments", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1005, "Invalid request", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND(1006, "Resource not found", HttpStatus.NOT_FOUND),
    CONFLICT(1007, "Resource already exists", HttpStatus.CONFLICT),
    FORBIDDEN(1008, "Access forbidden", HttpStatus.FORBIDDEN),
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized exception.", HttpStatus.INTERNAL_SERVER_ERROR),

    // Authentication Errors
    AUTH_TYPE_REQUIRED(2001, "Auth type is required", HttpStatus.BAD_REQUEST),
    INVALID_AUTH_TYPE(2002, "Invalid auth type", HttpStatus.BAD_REQUEST),
    INVALID_USERNAME_PASSWORD(2003, "Invalid username or password", HttpStatus.UNAUTHORIZED),
    OAUTH2_AUTO_REGISTRATION(2004, "OAuth2 users are registered automatically on first login", HttpStatus.BAD_REQUEST),
    INVALID_KEYCLOAK_TOKEN(2005, "Invalid Keycloak token", HttpStatus.UNAUTHORIZED),
    INVALID_REFRESH_TOKEN(2006, "Invalid or expired refresh token", HttpStatus.UNAUTHORIZED),

    // Authorization Errors
    ADMIN_ONLY_OPERATION(3001, "Only admin can create new accounts", HttpStatus.FORBIDDEN),
    USER_ACCOUNT_DISABLED(3002, "User account is disabled", HttpStatus.FORBIDDEN),
    USER_NOT_REGISTERED_FOR_AUTH(3003, "User is not registered for this auth type", HttpStatus.FORBIDDEN),

    // User Management Errors
    USERNAME_EXISTS(4001, "Username already exists", HttpStatus.CONFLICT),
    EMAIL_EXISTS(4002, "Email already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(4003, "User not found", HttpStatus.NOT_FOUND),

    // Dispatch / Technician Errors
    TECHNICIAN_NOT_QUALIFIED(5001, "Technician does not have the required skill for this ticket category", HttpStatus.BAD_REQUEST),
    TECHNICIAN_UNAVAILABLE(5002, "Technician is currently unavailable", HttpStatus.BAD_REQUEST),
    TECHNICIAN_AT_CAPACITY(5003, "Technician has reached maximum concurrent job limit — use forceAssign=true to override", HttpStatus.BAD_REQUEST),
    NO_QUALIFIED_TECHNICIAN(5004, "No available and qualified technician found for this ticket category", HttpStatus.NOT_FOUND),
    SKILL_ALREADY_EXISTS(5005, "Technician already has a skill record for this category", HttpStatus.CONFLICT),

    // Room Manager Errors
    NOT_ROOM_MANAGER(6001, "You are not the assigned manager for this room", HttpStatus.FORBIDDEN),
    ROOM_HAS_NO_MANAGER(6002, "This room does not have an assigned manager", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String errorMessage;
    private final HttpStatus httpStatus;
}


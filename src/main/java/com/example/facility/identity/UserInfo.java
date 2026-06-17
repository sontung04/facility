package com.example.facility.identity;

/**
 * Flat projection of a user, used by other modules that need user data
 * without importing the internal {@code identity.model.User} entity.
 */
public record UserInfo(Long id, String username, String role) {}

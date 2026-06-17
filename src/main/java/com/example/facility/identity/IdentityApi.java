package com.example.facility.identity;

import java.util.List;

/**
 * Public API of the Identity module.
 * Used by other modules that need to resolve or validate user data
 * without importing internal {@code identity.model} or {@code identity.repository} types.
 */
public interface IdentityApi {

    /**
     * Returns the DB ID for the given username.
     * Throws {@code WebException(USER_NOT_FOUND)} if no such user exists.
     */
    Long getUserIdByUsername(String username);

    /**
     * Returns a lightweight {@link UserInfo} projection for the given user ID.
     * Throws {@code WebException(USER_NOT_FOUND)} if not found.
     */
    UserInfo findById(Long userId);

    /**
     * Batch-loads {@link UserInfo} projections. Silently omits IDs not present in the DB.
     */
    List<UserInfo> findAllById(List<Long> userIds);

    /**
     * Returns IDs of all users that carry the given role name (e.g. {@code "ADMIN"}).
     */
    List<Long> findUserIdsByRole(String role);
}

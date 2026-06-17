/**
 * Identity module — user registration, authentication (JWT + OAuth2), and role management.
 *
 * <p>Public API exposed to other modules:
 * <ul>
 *   <li>{@link com.example.facility.identity.IdentityApi} — user lookups by ID / username / role</li>
 *   <li>{@link com.example.facility.identity.UserInfo} — lightweight user projection</li>
 * </ul>
 *
 * <p>Internal sub-packages ({@code model}, {@code repository}, {@code service},
 * {@code controller}, {@code dto}, {@code util}) must not be imported by other modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Identity",
        allowedDependencies = { "shared" }
)
package com.example.facility.identity;

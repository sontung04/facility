package com.example.facility.identity.model;

public enum UserRole {
    ADMIN,
    MANAGER,
    TECHNICIAN,
    @Deprecated REQUESTER,  // replaced by MANAGER — kept for DB safety during migration
    @Deprecated USER        // migrated to REQUESTER via Liquibase changeset 14; kept for DB safety only
}


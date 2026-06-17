package com.example.facility.facility.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persistent definition of a 3D furniture/object type.
 *
 * Built-in (system) types are seeded via Liquibase and cannot be deleted.
 * Admins can add custom types and edit the mesh config of any type at runtime.
 * {@code meshConfigJson} is a JSON array of primitive mesh components that the
 * React renderer turns into Three.js geometry at runtime.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "furniture_types")
public class FurnitureType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique identifier used as SceneObject.type on the frontend. */
    @Column(name = "type_code", unique = true, nullable = false, length = 100)
    private String typeCode;

    @Column(nullable = false, length = 200)
    private String label;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Hex colour used for the type badge / default mesh tint. */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String color = "#6b7280";

    /**
     * True for the 6 built-in types — cannot be deleted via the API.
     * Admins can still edit label, colour, and meshConfigJson.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean system = false;

    /**
     * JSON array of MeshComponent objects.
     * Empty array → renderer falls back to hardcoded ProceduralFurniture (built-ins)
     * or a plain grey box placeholder (custom types with no mesh defined yet).
     */
    @Column(name = "mesh_config_json", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String meshConfigJson = "[]";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

package com.example.facility.facility.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persists the 3D room layout for one room as a JSON blob.
 * Uses the room's PK as its own PK (shared-primary-key one-to-one).
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room_scenes")
public class RoomScene {

    @Id
    @Column(name = "room_id")
    private Long roomId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false)
    private Double width;

    @Column(nullable = false)
    private Double height;

    @Column(nullable = false)
    private Double length;

    /** Serialised JSON array of SceneObject — stored as TEXT. */
    @Column(name = "objects_json", columnDefinition = "TEXT", nullable = false)
    @Builder.Default
    private String objectsJson = "[]";

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

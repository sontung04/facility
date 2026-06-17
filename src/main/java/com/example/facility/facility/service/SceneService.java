package com.example.facility.facility.service;

import com.example.facility.facility.dto.request.SaveSceneRequest;
import com.example.facility.facility.dto.response.SceneResponse;
import com.example.facility.facility.model.Room;
import com.example.facility.facility.model.RoomScene;
import com.example.facility.facility.repository.RoomRepository;
import com.example.facility.facility.repository.RoomSceneRepository;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SceneService {

    private final RoomSceneRepository sceneRepository;
    private final RoomRepository roomRepository;
    private final ObjectMapper objectMapper;

    // ── Read ──────────────────────────────────────────────────────────────────

    /**
     * Returns the scene for a room, or {@code null} if no scene has been saved yet.
     */
    @Transactional(readOnly = true)
    public SceneResponse getScene(Long roomId) {
        return sceneRepository.findByRoom_Id(roomId)
                .map(this::toResponse)
                .orElse(null);
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    /**
     * Creates or fully replaces the scene for a room.
     * Only the manager responsible for the room (or an ADMIN) may call this.
     */
    @Transactional
    public SceneResponse saveScene(Long roomId, SaveSceneRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        String objectsJson = serialise(request.getObjects());

        RoomScene scene = sceneRepository.findByRoom_Id(roomId)
                .orElseGet(() -> RoomScene.builder().room(room).build());

        scene.setWidth(request.getWidth() != null ? request.getWidth() : 10.0);
        scene.setHeight(request.getHeight() != null ? request.getHeight() : 3.0);
        scene.setLength(request.getLength() != null ? request.getLength() : 10.0);
        scene.setObjectsJson(objectsJson);

        return toResponse(sceneRepository.save(scene));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteScene(Long roomId) {
        if (!sceneRepository.existsByRoom_Id(roomId)) {
            throw new WebException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        sceneRepository.deleteByRoom_Id(roomId);
        log.info("Deleted scene for room {}", roomId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String serialise(Object objects) {
        if (objects == null) return "[]";
        try {
            return objectMapper.writeValueAsString(objects);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise scene objects, using empty array", e);
            return "[]";
        }
    }

    private SceneResponse toResponse(RoomScene scene) {
        return SceneResponse.builder()
                .roomId(scene.getRoomId())
                .width(scene.getWidth())
                .height(scene.getHeight())
                .length(scene.getLength())
                .objects(scene.getObjectsJson())
                .updatedAt(scene.getUpdatedAt())
                .build();
    }
}

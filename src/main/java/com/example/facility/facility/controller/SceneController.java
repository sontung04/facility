package com.example.facility.facility.controller;

import com.example.facility.facility.dto.request.SaveSceneRequest;
import com.example.facility.facility.dto.response.SceneResponse;
import com.example.facility.facility.service.SceneService;
import com.example.facility.shared.apiresponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/facilities/rooms/{roomId}/scene")
@RequiredArgsConstructor
public class SceneController {

    private final SceneService sceneService;

    /**
     * GET — returns the room's current scene, or null data if none exists yet.
     * All authenticated roles may view.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<SceneResponse>> getScene(@PathVariable Long roomId) {
        SceneResponse scene = sceneService.getScene(roomId);
        return ResponseEntity.ok(ApiResponse.success("Scene retrieved successfully", scene));
    }

    /**
     * PUT — create or fully replace the scene for a room.
     * Only the MANAGER role (or ADMIN) may modify scenes.
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<SceneResponse>> saveScene(
            @PathVariable Long roomId,
            @RequestBody SaveSceneRequest request) {
        SceneResponse scene = sceneService.saveScene(roomId, request);
        return ResponseEntity.ok(ApiResponse.success("Scene saved successfully", scene));
    }

    /**
     * DELETE — remove the scene for a room.
     * MANAGER or ADMIN only.
     */
    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteScene(@PathVariable Long roomId) {
        sceneService.deleteScene(roomId);
        return ResponseEntity.ok(ApiResponse.success("Scene deleted successfully", null));
    }
}

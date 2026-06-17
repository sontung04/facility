package com.example.facility.facility.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.facility.dto.request.CreateRoomRequest;
import com.example.facility.facility.dto.response.RoomResponse;
import com.example.facility.facility.service.RoomService;
import com.example.facility.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @RequestBody CreateRoomRequest request) {
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Room created successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Room retrieved successfully",
                roomService.getRoomById(id)));
    }

    @GetMapping("/building/{buildingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByBuilding(
            @PathVariable Long buildingId) {
        return ResponseEntity.ok(ApiResponse.success("Rooms retrieved successfully",
                roomService.getRoomsByBuilding(buildingId)));
    }

    @GetMapping("/building/{buildingId}/floor/{floorNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByBuildingAndFloor(
            @PathVariable Long buildingId, @PathVariable Integer floorNumber) {
        return ResponseEntity.ok(ApiResponse.success("Rooms retrieved successfully",
                roomService.getRoomsByBuildingAndFloor(buildingId, floorNumber)));
    }

    /** List all rooms the currently authenticated MANAGER is responsible for. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getMyRooms() {
        Long managerId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success("Your rooms retrieved",
                roomService.getMyRooms(managerId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id, @RequestBody CreateRoomRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Room updated successfully",
                roomService.updateRoom(id, request)));
    }

    /** Assign a MANAGER user to a room. */
    @PutMapping("/{roomId}/manager/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> assignManager(
            @PathVariable Long roomId, @PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.success("Manager assigned successfully",
                roomService.assignManager(roomId, userId)));
    }

    /** Remove the manager assignment from a room. */
    @DeleteMapping("/{roomId}/manager")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> removeManager(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success("Manager removed",
                roomService.removeManager(roomId)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted successfully", null));
    }
}


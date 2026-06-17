package com.example.facility.facility.service;

import com.example.facility.facility.dto.request.CreateRoomRequest;
import com.example.facility.facility.dto.response.RoomResponse;
import com.example.facility.facility.model.Building;
import com.example.facility.facility.model.Room;
import com.example.facility.facility.repository.BuildingRepository;
import com.example.facility.facility.repository.RoomRepository;
import com.example.facility.identity.IdentityApi;
import com.example.facility.identity.UserInfo;
import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final IdentityApi identityApi;

    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        Building building = buildingRepository.findById(request.getBuildingId())
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        Room room = new Room();
        room.setBuilding(building);
        room.setName(request.getName());
        room.setFloorNumber(request.getFloorNumber());
        room.setDescription(request.getDescription());
        return mapToResponse(roomRepository.save(room));
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        return mapToResponse(roomRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByBuilding(Long buildingId) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new WebException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return roomRepository.findByBuildingId(buildingId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByBuildingAndFloor(Long buildingId, Integer floorNumber) {
        if (!buildingRepository.existsById(buildingId)) {
            throw new WebException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        return roomRepository.findByBuildingIdAndFloorNumber(buildingId, floorNumber)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public RoomResponse updateRoom(Long id, CreateRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        if (request.getBuildingId() != null) {
            Building building = buildingRepository.findById(request.getBuildingId())
                    .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
            room.setBuilding(building);
        }
        if (request.getName() != null)        room.setName(request.getName());
        if (request.getFloorNumber() != null)  room.setFloorNumber(request.getFloorNumber());
        if (request.getDescription() != null)  room.setDescription(request.getDescription());

        return mapToResponse(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        roomRepository.delete(room);
    }

    // ── Manager assignment ────────────────────────────────────────────────────

    /**
     * Assign (or reassign) a MANAGER user to a room.
     * Only users with role MANAGER may be assigned.
     */
    @Transactional
    public RoomResponse assignManager(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        UserInfo userInfo = identityApi.findById(userId); // throws USER_NOT_FOUND if absent
        if (!"MANAGER".equals(userInfo.role())) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        room.setManagerId(userId);
        log.info("Assigned manager {} to room {}", userInfo.username(), room.getName());
        return mapToResponse(roomRepository.save(room));
    }

    /** Remove the manager assignment from a room. */
    @Transactional
    public RoomResponse removeManager(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        room.setManagerId(null);
        return mapToResponse(roomRepository.save(room));
    }

    /** List all rooms that a manager is responsible for. */
    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms(Long managerId) {
        return roomRepository.findByManagerId(managerId)
                .stream().map(this::mapToResponse).toList();
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    RoomResponse mapToResponse(Room room) {
        // Batch-loads the single manager entry; returns null username if manager not found
        String managerUsername = null;
        if (room.getManagerId() != null) {
            managerUsername = identityApi.findAllById(List.of(room.getManagerId()))
                    .stream().findFirst().map(UserInfo::username).orElse(null);
        }
        return RoomResponse.builder()
                .id(room.getId())
                .buildingId(room.getBuilding().getId())
                .buildingName(room.getBuilding().getName())
                .name(room.getName())
                .floorNumber(room.getFloorNumber())
                .description(room.getDescription())
                .managerId(room.getManagerId())
                .managerUsername(managerUsername)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }
}

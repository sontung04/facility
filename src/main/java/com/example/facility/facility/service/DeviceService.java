package com.example.facility.facility.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.facility.dto.request.DeviceRequest;
import com.example.facility.facility.dto.response.DeviceResponse;
import com.example.facility.facility.model.Device;
import com.example.facility.facility.model.Room;
import com.example.facility.facility.repository.DeviceRepository;
import com.example.facility.facility.repository.RoomRepository;
import com.example.facility.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    @Transactional
    public DeviceResponse createDevice(DeviceRequest request) {
        try {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

            // If the caller is a MANAGER, verify they manage this specific room
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isManager = auth != null && auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER"));
            if (isManager) {
                String username = auth.getName();
                Long callerId = userRepository.findByUsername(username)
                        .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND))
                        .getId();
                if (!callerId.equals(room.getManagerId())) {
                    throw new WebException(ErrorCode.NOT_ROOM_MANAGER);
                }
            }

            Device device = new Device();
            device.setDeviceCode(request.getDeviceCode());
            device.setName(request.getName());
            device.setDeviceType(request.getDeviceType());
            device.setDescription(request.getDescription());
            device.setRoom(room);
            device.setLocationX(request.getLocationX());
            device.setLocationY(request.getLocationY());
            device.setLocationZ(request.getLocationZ());

            Device saved = deviceRepository.save(device);
            return mapToResponse(saved);
        } catch (WebException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating device", e);
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }
    }

    public DeviceResponse getDeviceById(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponse(device);
    }

    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    public List<DeviceResponse> getDevicesByRoom(Long roomId) {
        roomRepository.findById(roomId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        return deviceRepository.findByRoomId(roomId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public DeviceResponse updateDevice(Long id, DeviceRequest request) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        if (request.getDeviceCode() != null)
            device.setDeviceCode(request.getDeviceCode());
        if (request.getName() != null)
            device.setName(request.getName());
        if (request.getDeviceType() != null)
            device.setDeviceType(request.getDeviceType());
        if (request.getDescription() != null)
            device.setDescription(request.getDescription());
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
            device.setRoom(room);
        }
        if (request.getLocationX() != null)
            device.setLocationX(request.getLocationX());
        if (request.getLocationY() != null)
            device.setLocationY(request.getLocationY());
        if (request.getLocationZ() != null)
            device.setLocationZ(request.getLocationZ());

        Device saved = deviceRepository.save(device);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteDevice(Long id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        deviceRepository.delete(device);
    }

    private DeviceResponse mapToResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .roomId(device.getRoom().getId())
                .roomName(device.getRoom().getName())
                .deviceCode(device.getDeviceCode())
                .name(device.getName())
                .deviceType(device.getDeviceType())
                .description(device.getDescription())
                .locationX(device.getLocationX())
                .locationY(device.getLocationY())
                .locationZ(device.getLocationZ())
                .enabled(device.getEnabled())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}


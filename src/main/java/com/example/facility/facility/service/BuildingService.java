package com.example.facility.facility.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.facility.dto.request.BuildingRequest;
import com.example.facility.facility.dto.response.BuildingResponse;
import com.example.facility.facility.model.Building;
import com.example.facility.facility.repository.BuildingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuildingService {

    private final BuildingRepository buildingRepository;

    @Transactional
    public BuildingResponse createBuilding(BuildingRequest request) {
        try {
            Building building = new Building();
            building.setName(request.getName());
            building.setLocation(request.getLocation());
            building.setDescription(request.getDescription());

            Building saved = buildingRepository.save(building);
            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Error creating building", e);
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }
    }

    public BuildingResponse getBuildingById(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        return mapToResponse(building);
    }

    public List<BuildingResponse> getAllBuildings() {
        return buildingRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public BuildingResponse updateBuilding(Long id, BuildingRequest request) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        if (request.getName() != null)
            building.setName(request.getName());
        if (request.getLocation() != null)
            building.setLocation(request.getLocation());
        if (request.getDescription() != null)
            building.setDescription(request.getDescription());

        Building saved = buildingRepository.save(building);
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteBuilding(Long id) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        buildingRepository.delete(building);
    }

    private BuildingResponse mapToResponse(Building building) {
        return BuildingResponse.builder()
                .id(building.getId())
                .name(building.getName())
                .location(building.getLocation())
                .description(building.getDescription())
                .enabled(building.getEnabled())
                .createdAt(building.getCreatedAt())
                .updatedAt(building.getUpdatedAt())
                .build();
    }
}


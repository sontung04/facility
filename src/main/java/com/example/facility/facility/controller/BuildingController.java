package com.example.facility.facility.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.facility.dto.request.BuildingRequest;
import com.example.facility.facility.dto.response.BuildingResponse;
import com.example.facility.facility.service.BuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities/buildings")
@RequiredArgsConstructor
public class BuildingController {

    private final BuildingService buildingService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BuildingResponse>> createBuilding(@RequestBody BuildingRequest request) {
        BuildingResponse response = buildingService.createBuilding(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Building created successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getAllBuildings() {
        List<BuildingResponse> buildings = buildingService.getAllBuildings();
        return ResponseEntity.ok(ApiResponse.success("Buildings retrieved successfully", buildings));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<BuildingResponse>> getBuildingById(@PathVariable Long id) {
        BuildingResponse building = buildingService.getBuildingById(id);
        return ResponseEntity.ok(ApiResponse.success("Building retrieved successfully", building));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BuildingResponse>> updateBuilding(
            @PathVariable Long id, @RequestBody BuildingRequest request) {
        BuildingResponse response = buildingService.updateBuilding(id, request);
        return ResponseEntity.ok(ApiResponse.success("Building updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBuilding(@PathVariable Long id) {
        buildingService.deleteBuilding(id);
        return ResponseEntity.ok(ApiResponse.success("Building deleted successfully", null));
    }
}


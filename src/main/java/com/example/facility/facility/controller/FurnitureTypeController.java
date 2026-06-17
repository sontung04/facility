package com.example.facility.facility.controller;

import com.example.facility.facility.dto.request.FurnitureTypeRequest;
import com.example.facility.facility.dto.response.FurnitureTypeResponse;
import com.example.facility.facility.service.FurnitureTypeService;
import com.example.facility.shared.apiresponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for runtime furniture-type definitions.
 *
 * GET  /api/v1/furniture-types        — all authenticated roles
 * GET  /api/v1/furniture-types/{id}   — all authenticated roles
 * POST /api/v1/furniture-types        — ADMIN only
 * PUT  /api/v1/furniture-types/{id}   — ADMIN only
 * DELETE /api/v1/furniture-types/{id} — ADMIN only (system types rejected)
 *
 * Built-in types are seeded by Liquibase (V13__furniture_types.yaml).
 * HTTP-layer rules in SecurityConfig mirror the @PreAuthorize checks for
 * belt-and-suspenders protection.
 */
@RestController
@RequestMapping("/api/v1/furniture-types")
@RequiredArgsConstructor
public class FurnitureTypeController {

    private final FurnitureTypeService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<FurnitureTypeResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(
                "Furniture types retrieved successfully", service.listAll()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<FurnitureTypeResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Furniture type retrieved successfully", service.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FurnitureTypeResponse>> create(
            @RequestBody FurnitureTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Furniture type created successfully", service.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<FurnitureTypeResponse>> update(
            @PathVariable Long id, @RequestBody FurnitureTypeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(
                "Furniture type updated successfully", service.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Furniture type deleted successfully", null));
    }
}

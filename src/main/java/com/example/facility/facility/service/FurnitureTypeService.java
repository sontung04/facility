package com.example.facility.facility.service;

import com.example.facility.facility.dto.request.FurnitureTypeRequest;
import com.example.facility.facility.dto.response.FurnitureTypeResponse;
import com.example.facility.facility.model.FurnitureType;
import com.example.facility.facility.repository.FurnitureTypeRepository;
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
public class FurnitureTypeService {

    private final FurnitureTypeRepository repository;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FurnitureTypeResponse> listAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FurnitureTypeResponse getById(Long id) {
        return toResponse(repository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND)));
    }

    // ── Write (ADMIN only — enforced at controller + HTTP-security level) ─────

    @Transactional
    public FurnitureTypeResponse create(FurnitureTypeRequest req) {
        if (req.getTypeCode() == null || req.getTypeCode().isBlank())
            throw new WebException(ErrorCode.INVALID_REQUEST);
        if (req.getLabel() == null || req.getLabel().isBlank())
            throw new WebException(ErrorCode.INVALID_REQUEST);

        String code = req.getTypeCode().toUpperCase().replaceAll("[^A-Z0-9_]", "_");
        if (repository.existsByTypeCode(code))
            throw new WebException(ErrorCode.CONFLICT);

        FurnitureType ft = FurnitureType.builder()
                .typeCode(code)
                .label(req.getLabel())
                .description(req.getDescription())
                .color(req.getColor() != null ? req.getColor() : "#6b7280")
                .system(false)
                .meshConfigJson(req.getMeshConfigJson() != null ? req.getMeshConfigJson() : "[]")
                .build();
        return toResponse(repository.save(ft));
    }

    @Transactional
    public FurnitureTypeResponse update(Long id, FurnitureTypeRequest req) {
        FurnitureType ft = repository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        // typeCode is immutable — never updated
        if (req.getLabel() != null && !req.getLabel().isBlank())
            ft.setLabel(req.getLabel());
        if (req.getDescription() != null)
            ft.setDescription(req.getDescription());
        if (req.getColor() != null && !req.getColor().isBlank())
            ft.setColor(req.getColor());
        if (req.getMeshConfigJson() != null)
            ft.setMeshConfigJson(req.getMeshConfigJson());

        return toResponse(repository.save(ft));
    }

    @Transactional
    public void delete(Long id) {
        FurnitureType ft = repository.findById(id)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
        if (ft.isSystem())
            throw new WebException(ErrorCode.FORBIDDEN);
        repository.delete(ft);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private FurnitureTypeResponse toResponse(FurnitureType ft) {
        return FurnitureTypeResponse.builder()
                .id(ft.getId())
                .typeCode(ft.getTypeCode())
                .label(ft.getLabel())
                .description(ft.getDescription())
                .color(ft.getColor())
                .system(ft.isSystem())
                .meshConfigJson(ft.getMeshConfigJson())
                .createdAt(ft.getCreatedAt())
                .updatedAt(ft.getUpdatedAt())
                .build();
    }
}

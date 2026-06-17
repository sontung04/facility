package com.example.facility.facility.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class FurnitureTypeResponse {
    private Long          id;
    private String        typeCode;
    private String        label;
    private String        description;
    private String        color;
    private boolean       system;
    private String        meshConfigJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

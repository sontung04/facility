package com.example.facility.facility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private Long buildingId;
    private String buildingName;
    private String name;
    private Integer floorNumber;
    private String description;
    private Long managerId;
    private String managerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


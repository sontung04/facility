package com.example.facility.facility.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    private Long buildingId;
    private String name;
    private Integer floorNumber;
    private String description;
}


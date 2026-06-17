package com.example.facility.facility.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private String deviceCode;
    private String name;
    private String deviceType;
    private String description;
    private Float locationX;
    private Float locationY;
    private Float locationZ;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


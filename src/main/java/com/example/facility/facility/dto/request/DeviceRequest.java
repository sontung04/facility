package com.example.facility.facility.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRequest {
    private Long roomId;
    private String deviceCode;
    private String name;
    private String deviceType;
    private String description;
    private Float locationX;
    private Float locationY;
    private Float locationZ;
}


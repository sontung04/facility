package com.example.facility.facility.dto.request;

import lombok.Data;

@Data
public class FurnitureTypeRequest {
    private String typeCode;       // required on create; ignored on update
    private String label;          // required on create
    private String description;
    private String color;
    private String meshConfigJson; // JSON array string
}

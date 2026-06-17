package com.example.facility.dispatch.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TechnicianSkillRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "categoryId is required")
    private Long categoryId;

    /** BEGINNER | INTERMEDIATE | ADVANCED | EXPERT */
    @NotNull(message = "skillLevel is required")
    private String skillLevel;

    @Min(value = 1, message = "maxConcurrentJobs must be at least 1")
    private Integer maxConcurrentJobs = 5;

    private boolean available = true;
}



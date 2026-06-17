package com.example.facility.dispatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TechnicianSkillResponse {

    private Long id;

    private Long userId;
    private String username;

    private Long categoryId;
    private String categoryName;

    /** BEGINNER | INTERMEDIATE | ADVANCED | EXPERT */
    private String skillLevel;

    private Integer maxConcurrentJobs;
    private boolean available;

    private LocalDateTime createdAt;
}



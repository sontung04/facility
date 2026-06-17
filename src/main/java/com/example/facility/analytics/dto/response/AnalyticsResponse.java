package com.example.facility.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsResponse {

    private Long totalTickets;
    private Long resolvedTickets;
    private Long pendingTickets;
    private Double averageMTTR; // in minutes — from SUBMITTED to RESOLVED
    private Double slaBreachRate; // percentage
    private Long slaBreachCount;
    private Long totalTechnicians;
    private Double averageAssignmentLoad;
    private LocalDateTime calculatedAt;
}


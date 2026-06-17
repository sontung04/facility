package com.example.facility.dispatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianPerformanceResponse {
    private Long technicianId;
    private Long resolvedCount;
    private Double avgResolutionTimeMinutes;
    private Double slaComplianceRate;
    private Double avgRating;
    private Double qualityScore;
    private Long activeTicketCount;
}


package com.example.facility.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SLABreachDetailResponse {
    private Long breachId;
    private Long ticketId;
    private String ticketNumber;
    private String breachType;
    private LocalDateTime expectedBy;
    private LocalDateTime actualBreachAt;
    private String categoryName;
    private String deviceCode;
    private String buildingName;
    private String severityLevel;
}


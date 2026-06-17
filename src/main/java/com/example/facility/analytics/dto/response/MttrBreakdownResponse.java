package com.example.facility.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MttrBreakdownResponse {
    private Long groupId;
    private String groupName;
    private Double avgMttrMinutes;
    private Long ticketCount;
}


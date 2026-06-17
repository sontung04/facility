package com.example.facility.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketVolumeResponse {
    private String period;  // e.g. "2026-05-01" (DAY), "2026-05-18" (week start), "2026-05-01" (MONTH)
    private Long count;
}


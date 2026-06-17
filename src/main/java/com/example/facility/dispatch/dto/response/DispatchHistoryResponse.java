package com.example.facility.dispatch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchHistoryResponse {
    private Long id;
    private Long ticketId;
    private Long technicianId;
    private Long previousTechnicianId;
    private Long dispatcherId;
    private String dispatchType;
    private String notes;
    private LocalDateTime createdAt;
}


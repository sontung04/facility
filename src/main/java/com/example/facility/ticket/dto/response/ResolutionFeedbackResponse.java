package com.example.facility.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionFeedbackResponse {
    private Long id;
    private Long ticketId;
    private Long technicianId;
    private Long ratedBy;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}


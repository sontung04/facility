package com.example.facility.ticket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionFeedbackRequest {
    private Integer rating;  // 1–5
    private String comment;
}


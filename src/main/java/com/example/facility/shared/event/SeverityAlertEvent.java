package com.example.facility.shared.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeverityAlertEvent {
    private Long ticketId;
    private String ticketNumber;
    private String previousSeverityLevel;
    private String newSeverityLevel;
    private Float previousSeverityScore;
    private Float newSeverityScore;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime escalatedAt;
}


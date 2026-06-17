package com.example.facility.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private String status;
    private String severityLevel;
    private Float severityScore;
    private String description;
    private Long deviceId;
    private String deviceCode;
    private Long categoryId;
    private String categoryName;
    private Long reportedBy;
    private Long assignedTechnicianId;
    private String assignmentNotes;
    private LocalDateTime submittedAt;
    private LocalDateTime ackAt;
    private LocalDateTime assignedAt;
    private LocalDateTime inProgressAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;
    private Boolean slaAckBreached;
    private Boolean slaResolveBreached;
    private Boolean slaClosureBreached;
}


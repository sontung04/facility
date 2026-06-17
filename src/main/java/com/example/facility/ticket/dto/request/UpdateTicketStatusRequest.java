package com.example.facility.ticket.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {
    private Long ticketId;
    private String newStatus; // SUBMITTED, ACK, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED
    private String notes;
}




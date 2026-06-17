package com.example.facility.dispatch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignTicketRequest {
    private Long ticketId;
    private Long technicianId;
    private String notes;
    private boolean autoAssign;   // if true, system ignores technicianId and auto-selects
    private boolean forceAssign;  // if true, bypass capacity check in manual assign
}




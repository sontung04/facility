package com.example.facility.dispatch.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.dispatch.dto.request.AssignTicketRequest;
import com.example.facility.dispatch.dto.response.DispatchHistoryResponse;
import com.example.facility.dispatch.dto.response.TechnicianPerformanceResponse;
import com.example.facility.dispatch.service.DispatchService;
import com.example.facility.dispatch.service.TechnicianPerformanceService;
import com.example.facility.ticket.TicketSummary;
import com.example.facility.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;
    private final TechnicianPerformanceService performanceService;
    private final SecurityUtils securityUtils;

    @PostMapping("/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TicketSummary>> assignTicket(
            @RequestBody AssignTicketRequest request) {
        Long dispatcherId = securityUtils.getCurrentUserId();
        TicketSummary ticket = dispatchService.assignTicket(request, dispatcherId);
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned successfully", ticket));
    }

    // toggle technician availability (marks all their skill records)
    @PutMapping("/technicians/{userId}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> setAvailability(
            @PathVariable Long userId,
            @RequestParam boolean available) {
        dispatchService.setTechnicianAvailability(userId, available);
        return ResponseEntity.ok(ApiResponse.success(
                "Technician availability set to " + available, null));
    }

    // assignment history for a ticket
    @GetMapping("/history/ticket/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<DispatchHistoryResponse>>> getHistoryByTicket(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                dispatchService.getHistoryByTicket(ticketId)));
    }

    // assignment history for a technician
    @GetMapping("/history/technician/{technicianId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<DispatchHistoryResponse>>> getHistoryByTechnician(
            @PathVariable Long technicianId) {
        return ResponseEntity.ok(ApiResponse.success("History retrieved",
                dispatchService.getHistoryByTechnician(technicianId)));
    }

    // per-technician performance metrics
    @GetMapping("/technicians/{technicianId}/performance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TechnicianPerformanceResponse>> getTechnicianPerformance(
            @PathVariable Long technicianId) {
        return ResponseEntity.ok(ApiResponse.success("Performance retrieved",
                performanceService.getTechnicianPerformance(technicianId)));
    }
}


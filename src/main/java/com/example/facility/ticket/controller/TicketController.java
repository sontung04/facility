package com.example.facility.ticket.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.ticket.dto.request.CreateTicketRequest;
import com.example.facility.ticket.dto.request.UpdateTicketStatusRequest;
import com.example.facility.ticket.dto.response.TicketResponse;
import com.example.facility.ticket.service.TicketService;
import com.example.facility.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<TicketResponse>> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        Long managerId = securityUtils.getCurrentUserId();
        TicketResponse ticket = ticketService.createTicket(request, managerId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket created successfully", ticket));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> getTicket(@PathVariable Long ticketId) {
        TicketResponse ticket = ticketService.getTicket(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Ticket retrieved successfully", ticket));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getTicketsByStatus(
            @RequestParam String status,
            Pageable pageable) {
        Page<TicketResponse> tickets = ticketService.getTicketsByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Tickets retrieved successfully", tickets));
    }

    @GetMapping("/technician/my-tickets")
    @PreAuthorize("hasRole('TECHNICIAN')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyTickets(Pageable pageable) {
        Long technicianId = securityUtils.getCurrentUserId();
        Page<TicketResponse> tickets = ticketService.getTechnicianTickets(technicianId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Your tickets retrieved successfully", tickets));
    }

    @GetMapping("/my-reported")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> getMyReportedTickets(Pageable pageable) {
        Page<TicketResponse> tickets = ticketService.getManagerTickets(securityUtils.getCurrentUserId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Your reported tickets retrieved successfully", tickets));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> searchTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        Page<TicketResponse> tickets = ticketService.searchTickets(status, deviceId, categoryId,
                severity, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved successfully", tickets));
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('TECHNICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestBody UpdateTicketStatusRequest request) {
        request.setTicketId(ticketId);
        Long userId = securityUtils.getCurrentUserId();
        TicketResponse ticket = ticketService.updateTicketStatus(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Ticket status updated successfully", ticket));
    }
}


package com.example.facility.ticket.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.ticket.dto.request.ResolutionFeedbackRequest;
import com.example.facility.ticket.dto.response.ResolutionFeedbackResponse;
import com.example.facility.ticket.service.ResolutionFeedbackService;
import com.example.facility.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class ResolutionFeedbackController {

    private final ResolutionFeedbackService feedbackService;
    private final SecurityUtils securityUtils;

    // Room manager rates the resolution after the ticket is CLOSED
    @PostMapping("/{ticketId}/feedback")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<ResolutionFeedbackResponse>> submitFeedback(
            @PathVariable Long ticketId,
            @RequestBody ResolutionFeedbackRequest request) {
        Long managerId = securityUtils.getCurrentUserId();
        ResolutionFeedbackResponse response = feedbackService.submitFeedback(ticketId, request, managerId);
        return ResponseEntity.ok(ApiResponse.success("Feedback submitted successfully", response));
    }

    @GetMapping("/{ticketId}/feedback")
    @PreAuthorize("hasAnyRole('MANAGER', 'TECHNICIAN', 'ADMIN')")
    public ResponseEntity<ApiResponse<ResolutionFeedbackResponse>> getFeedback(
            @PathVariable Long ticketId) {
        return ResponseEntity.ok(ApiResponse.success("Feedback retrieved",
                feedbackService.getFeedback(ticketId)));
    }
}


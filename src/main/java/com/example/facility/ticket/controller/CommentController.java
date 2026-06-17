package com.example.facility.ticket.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.ticket.dto.request.CommentRequest;
import com.example.facility.ticket.dto.response.CommentResponse;
import com.example.facility.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long ticketId,
            @RequestBody CommentRequest request) {
        CommentResponse response = ticketService.addComment(ticketId, request.getText());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TECHNICIAN')")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(@PathVariable Long ticketId) {
        List<CommentResponse> comments = ticketService.getTicketComments(ticketId);
        return ResponseEntity.ok(ApiResponse.success("Comments retrieved successfully", comments));
    }
}


package com.example.facility.ticket.service;

import com.example.facility.shared.exception.ErrorCode;
import com.example.facility.shared.exception.WebException;
import com.example.facility.ticket.dto.request.ResolutionFeedbackRequest;
import com.example.facility.ticket.dto.response.ResolutionFeedbackResponse;
import com.example.facility.ticket.model.ResolutionFeedback;
import com.example.facility.ticket.model.Ticket;
import com.example.facility.ticket.model.TicketStatus;
import com.example.facility.ticket.repository.ResolutionFeedbackRepository;
import com.example.facility.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResolutionFeedbackService {

    private final TicketRepository ticketRepository;
    private final ResolutionFeedbackRepository feedbackRepository;

    @Transactional
    public ResolutionFeedbackResponse submitFeedback(Long ticketId, ResolutionFeedbackRequest request,
            Long requesterId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));

        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        // Only the original reporter may rate
        if (!requesterId.equals(ticket.getReportedBy())) {
            throw new WebException(ErrorCode.FORBIDDEN);
        }

        if (ticket.getAssignedTechnicianId() == null) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        if (feedbackRepository.findByTicketId(ticketId).isPresent()) {
            throw new WebException(ErrorCode.INVALID_REQUEST); // already rated
        }

        if (request.getRating() == null || request.getRating() < 1 || request.getRating() > 5) {
            throw new WebException(ErrorCode.INVALID_REQUEST);
        }

        ResolutionFeedback feedback = ResolutionFeedback.builder()
                .ticketId(ticketId)
                .technicianId(ticket.getAssignedTechnicianId())
                .ratedBy(requesterId)
                .rating(request.getRating())
                .comment(request.getComment())
                .build();

        feedback = feedbackRepository.save(feedback);
        return toResponse(feedback);
    }

    @Transactional(readOnly = true)
    public ResolutionFeedbackResponse getFeedback(Long ticketId) {
        return feedbackRepository.findByTicketId(ticketId)
                .map(this::toResponse)
                .orElseThrow(() -> new WebException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private ResolutionFeedbackResponse toResponse(ResolutionFeedback f) {
        return ResolutionFeedbackResponse.builder()
                .id(f.getId())
                .ticketId(f.getTicketId())
                .technicianId(f.getTechnicianId())
                .ratedBy(f.getRatedBy())
                .rating(f.getRating())
                .comment(f.getComment())
                .createdAt(f.getCreatedAt())
                .build();
    }
}


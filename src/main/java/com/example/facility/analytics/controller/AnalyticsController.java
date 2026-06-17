package com.example.facility.analytics.controller;

import com.example.facility.shared.apiresponse.ApiResponse;
import com.example.facility.analytics.dto.response.AnalyticsResponse;
import com.example.facility.analytics.dto.response.MttrBreakdownResponse;
import com.example.facility.analytics.dto.response.SLABreachDetailResponse;
import com.example.facility.analytics.dto.response.TicketVolumeResponse;
import com.example.facility.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // dashboard with filters 
    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsResponse>> getAnalyticsDashboard(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        AnalyticsResponse analytics = analyticsService.generateAnalytics(
                categoryId, buildingId, severity, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Analytics retrieved successfully", analytics));
    }

    // Existing SLA compliance 
    @GetMapping("/sla-compliance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> getSLACompliance(
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        LocalDateTime start = startDate != null ? startDate : LocalDateTime.now().minusDays(30);
        LocalDateTime end   = endDate   != null ? endDate   : LocalDateTime.now();
        Double complianceRate = analyticsService.calculateSLAComplianceRate(start, end);
        return ResponseEntity.ok(ApiResponse.success("SLA compliance calculated",
                String.format("%.2f%%", complianceRate)));
    }

    // ticket volume by period (L1 fix)
    // groupBy: DAY (default) | WEEK | MONTH
    @GetMapping("/ticket-volume")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<TicketVolumeResponse>>> getTicketVolume(
            @RequestParam(required = false, defaultValue = "DAY") String groupBy,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<TicketVolumeResponse> volume = analyticsService.getTicketVolumeByPeriod(
                groupBy, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Ticket volume retrieved successfully", volume));
    }

    // FR-RPT-02: MTTR breakdown (L2 fix)
    // groupBy: CATEGORY (default) | TECHNICIAN | BUILDING
    @GetMapping("/mttr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MttrBreakdownResponse>>> getMttrBreakdown(
            @RequestParam(required = false, defaultValue = "CATEGORY") String groupBy,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<MttrBreakdownResponse> breakdown = analyticsService.getMttrBreakdown(
                groupBy, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("MTTR breakdown retrieved successfully", breakdown));
    }

    // FR-RPT-03: SLA breach detail list (L3 fix)
    @GetMapping("/sla-breaches")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SLABreachDetailResponse>>> getSLABreachDetails(
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<SLABreachDetailResponse> details = analyticsService.getSLABreachDetails(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("SLA breach details retrieved successfully", details));
    }
}


package com.example.facility.audit.controller;

import com.example.facility.audit.dto.response.AuditLogResponse;
import com.example.facility.audit.service.AuditLogService;
import com.example.facility.shared.apiresponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST endpoint for paginated audit log access.
 *
 * GET /api/v1/audit-logs — ADMIN only
 *
 * Query params:
 *   action      (optional) — filter by exact action string
 *   entityType  (optional) — filter by entity type (e.g. "TICKET")
 *   userId      (optional) — filter by user ID
 *   from        (optional) — ISO datetime lower bound
 *   to          (optional) — ISO datetime upper bound
 *   page / size / sort — standard Pageable (default: page=0, size=20, sort=createdAt,desc)
 */
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService service;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> search(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<AuditLogResponse> page = service.search(action, entityType, userId, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", page));
    }
}

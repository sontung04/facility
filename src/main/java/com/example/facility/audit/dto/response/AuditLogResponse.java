package com.example.facility.audit.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long          id;
    private Long          userId;
    private String        username;
    private String        action;
    private String        entityType;
    private Long          entityId;
    private String        changes;
    private LocalDateTime createdAt;
}

package com.example.facility.audit.service;

import com.example.facility.audit.dto.response.AuditLogResponse;
import com.example.facility.audit.model.AuditLog;
import com.example.facility.audit.repository.AuditLogRepository;
import com.example.facility.identity.IdentityApi;
import com.example.facility.identity.UserInfo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository repository;
    private final IdentityApi        identityApi;

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(
            String action,
            String entityType,
            Long userId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null && !action.isBlank())
                predicates.add(cb.equal(root.get("action"), action.toUpperCase()));

            if (entityType != null && !entityType.isBlank())
                predicates.add(cb.equal(root.get("entityType"), entityType.toUpperCase()));

            if (userId != null)
                predicates.add(cb.equal(root.get("userId"), userId));

            if (from != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));

            if (to != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> page = repository.findAll(spec, pageable);

        // Batch-resolve usernames for this page
        List<Long> userIds = page.getContent().stream()
                .map(AuditLog::getUserId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> usernameById = identityApi.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserInfo::id, UserInfo::username));

        return page.map(log -> toResponse(log, usernameById));
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private AuditLogResponse toResponse(AuditLog log, Map<Long, String> usernameById) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(usernameById.getOrDefault(log.getUserId(), "user#" + log.getUserId()))
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .changes(log.getChanges())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

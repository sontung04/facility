package com.example.facility.sla.service;

import com.example.facility.sla.SLABreachSummary;
import com.example.facility.sla.SlaApi;
import com.example.facility.sla.repository.SLABreachRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// Date-range sentinel values used when the caller passes null dates.
// The JPQL queries never use (:param IS NULL OR ...) — that pattern fails on
// PostgreSQL (SQLState 42P18) because the driver sends null::unknown and
// Postgres cannot infer the column type.  Instead, null → epoch / far-future.

@Service
@RequiredArgsConstructor
public class SlaApiImpl implements SlaApi {

    private static final LocalDateTime EPOCH     = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2100, 1, 1, 0, 0);

    private final SLABreachRepository slaBreachRepository;

    @Override
    @Transactional(readOnly = true)
    public long countBreachedInRange(LocalDateTime startDate, LocalDateTime endDate) {
        return slaBreachRepository.countBreachedInRange(
                startDate != null ? startDate : EPOCH,
                endDate   != null ? endDate   : FAR_FUTURE);
    }

    @Override
    @Transactional(readOnly = true)
    public long countBreachedTicketsInRange(LocalDateTime startDate, LocalDateTime endDate) {
        return slaBreachRepository.countBreachedTicketsInRange(
                startDate != null ? startDate : EPOCH,
                endDate   != null ? endDate   : FAR_FUTURE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SLABreachSummary> findBreachedWithDetails(LocalDateTime startDate, LocalDateTime endDate) {
        return slaBreachRepository.findBreachedWithDetails(
                        startDate != null ? startDate : EPOCH,
                        endDate   != null ? endDate   : FAR_FUTURE).stream()
                .map(sb -> {
                    var t = sb.getTicket();
                    return new SLABreachSummary(
                            sb.getId(),
                            sb.getBreachType().name(),
                            sb.getExpectedBy(),
                            sb.getActualBreachAt(),
                            t.getId(),
                            t.getTicketNumber(),
                            t.getCategory().getName(),
                            t.getDevice().getDeviceCode(),
                            t.getDevice().getRoom().getBuilding().getName(),
                            t.getSeverityLevel().name()
                    );
                })
                .toList();
    }
}

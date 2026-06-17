package com.example.facility.ticket.repository;

import com.example.facility.ticket.model.SeverityLevel;
import com.example.facility.ticket.model.Ticket;
import com.example.facility.ticket.model.TicketStatus;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for flexible ticket filtering.
 *
 * <p>Replaces JPQL {@code (:param IS NULL OR col = :param)} patterns that fail
 * on PostgreSQL when a null is bound for enum or timestamp parameters —
 * PostgreSQL sends {@code SQLState 42P18: could not determine data type of $N}
 * because the driver passes {@code null::unknown} and Postgres cannot infer the
 * column type from the {@code ? IS NULL} check alone.
 *
 * <p>Specifications only add a predicate when the value is non-null, so
 * PostgreSQL never receives an untyped null parameter.
 *
 * <p>NOTE: {@code jakarta.persistence.criteria.Fetch} and {@code Join} are
 * unrelated interfaces in Jakarta Persistence 3.x — casting between them is a
 * compile error.  Fetch-join eagerness for analytics is handled via
 * {@code @EntityGraph} on {@link TicketRepository#findAllWithGraph}.
 */
public class TicketSpecification {

    private TicketSpecification() {}

    // ── General search (TicketService.searchTickets) ─────────────────────────

    public static Specification<Ticket> withFilters(
            TicketStatus status,
            Long deviceId,
            Long categoryId,
            SeverityLevel severityLevel,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (deviceId != null) {
                predicates.add(cb.equal(root.get("device").get("id"), deviceId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (severityLevel != null) {
                predicates.add(cb.equal(root.get("severityLevel"), severityLevel));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("submittedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("submittedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── Analytics filter (TicketApiImpl.findForAnalytics) ────────────────────
    //
    // Used with TicketRepository.findAllWithGraph() which carries an @EntityGraph
    // for category + device so toSummary() does not trigger N+1 queries.
    // The buildingId filter is satisfied by a regular join (not a fetch) through
    // device → room → building.

    public static Specification<Ticket> withAnalyticsFilters(
            Long categoryId,
            Long buildingId,
            SeverityLevel severityLevel,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (buildingId != null) {
                // Join device → room, then filter on building.id.
                // var avoids explicit generic type parameters (Join<Z,X> wildcards
                // complicate method-chaining when types are string-inferred).
                var deviceJoin = root.join("device", JoinType.INNER);
                var roomJoin   = deviceJoin.join("room", JoinType.INNER);
                predicates.add(cb.equal(roomJoin.get("building").get("id"), buildingId));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (severityLevel != null) {
                predicates.add(cb.equal(root.get("severityLevel"), severityLevel));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("submittedAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("submittedAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

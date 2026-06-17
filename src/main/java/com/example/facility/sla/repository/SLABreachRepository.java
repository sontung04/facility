package com.example.facility.sla.repository;

import com.example.facility.sla.model.SLABreach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SLABreachRepository extends JpaRepository<SLABreach, Long> {

    @Query("SELECT sb FROM SLABreach sb WHERE sb.isBreached = false AND sb.expectedBy <= :now")
    List<SLABreach> findPendingBreaches(LocalDateTime now);

    @Query("SELECT sb FROM SLABreach sb WHERE sb.ticket.id = :ticketId")
    List<SLABreach> findByTicketId(Long ticketId);

    @Query("SELECT COUNT(sb) FROM SLABreach sb WHERE sb.isBreached = true")
    Long countBreachedSLAs();

    // Analytics L3 (FR-RPT-03): full breach detail list with JOIN FETCH to prevent N+1.
    // Dates are always non-null: SlaApiImpl defaults null → epoch / far-future before calling.
    // (The old (:param IS NULL OR col = :param) pattern fails on PostgreSQL with SQLState 42P18.)
    @Query("SELECT sb FROM SLABreach sb " +
           "JOIN FETCH sb.ticket t " +
           "JOIN FETCH t.category " +
           "JOIN FETCH t.device d " +
           "JOIN FETCH d.room r " +
           "JOIN FETCH r.building " +
           "WHERE sb.isBreached = true " +
           "AND sb.createdAt >= :startDate " +
           "AND sb.createdAt <= :endDate " +
           "ORDER BY sb.createdAt DESC")
    List<SLABreach> findBreachedWithDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L3: count breach *records* within a date range.
    // Dates are always non-null: SlaApiImpl defaults null → epoch / far-future before calling.
    @Query("SELECT COUNT(sb) FROM SLABreach sb " +
           "WHERE sb.isBreached = true " +
           "AND sb.createdAt >= :startDate " +
           "AND sb.createdAt <= :endDate")
    Long countBreachedInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics: count *distinct tickets* that have at least one breach in range.
    // Used for breach-rate / compliance-rate calculations to avoid > 100% (a ticket
    // can generate up to 3 breach records: ACK, RESOLVE, CLOSURE).
    @Query("SELECT COUNT(DISTINCT sb.ticket.id) FROM SLABreach sb " +
           "WHERE sb.isBreached = true " +
           "AND sb.createdAt >= :startDate " +
           "AND sb.createdAt <= :endDate")
    Long countBreachedTicketsInRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}


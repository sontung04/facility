package com.example.facility.ticket.repository;

import com.example.facility.ticket.model.Ticket;
import com.example.facility.ticket.model.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    // Fix 3: deduplication now includes categoryId so different issues on the same device get separate tickets
    List<Ticket> findByDeviceIdAndCategoryIdAndStatusInAndSubmittedAtAfter(
            Long deviceId, Long categoryId, List<TicketStatus> statuses, LocalDateTime submittedAfter);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
    List<Ticket> findByStatus(TicketStatus status);

    Page<Ticket> findByAssignedTechnicianId(Long technicianId, Pageable pageable);
    List<Ticket> findByAssignedTechnicianId(Long technicianId);

    // Fix 6: requester can see tickets they originally filed
    Page<Ticket> findByReportedBy(Long reportedBy, Pageable pageable);

    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses AND t.slaAckBreached = false")
    List<Ticket> findPendingAckTickets(@Param("statuses") List<TicketStatus> statuses);

    @Query("SELECT t FROM Ticket t WHERE t.status IN :statuses AND t.slaResolveBreached = false")
    List<Ticket> findPendingResolveTickets(@Param("statuses") List<TicketStatus> statuses);

    List<Ticket> findByStatusIn(List<TicketStatus> statuses);

    List<Ticket> findBySubmittedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Fix 11: single aggregate query replaces per-technician loop
    @Query("SELECT t.assignedTechnicianId, COUNT(t) FROM Ticket t " +
           "WHERE t.assignedTechnicianId IN :ids " +
           "AND t.status NOT IN :closedStatuses " +
           "GROUP BY t.assignedTechnicianId")
    List<Object[]> countActiveTicketsByTechnicianIds(
            @Param("ids") List<Long> ids,
            @Param("closedStatuses") List<TicketStatus> closedStatuses);

    // L4: batch performance metrics — returns [technicianId, resolvedCount, slaBreachCount]
    @Query("SELECT t.assignedTechnicianId, COUNT(t), " +
           "SUM(CASE WHEN t.slaResolveBreached = true THEN 1L ELSE 0L END) " +
           "FROM Ticket t WHERE t.assignedTechnicianId IN :ids " +
           "AND t.status IN :closedStatuses GROUP BY t.assignedTechnicianId")
    List<Object[]> countPerformanceMetricsByTechnicianIds(
            @Param("ids") List<Long> ids,
            @Param("closedStatuses") List<TicketStatus> closedStatuses);

    // L4: full resolved tickets for single technician (avg resolution time computed in Java)
    List<Ticket> findByAssignedTechnicianIdAndStatusIn(Long technicianId, List<TicketStatus> statuses);

    // Overrides JpaSpecificationExecutor.findAll(Specification) to add @EntityGraph.
    // Fetches category + device eagerly so toSummary() (category.name, device.deviceCode)
    // does not generate N+1 queries on the analytics path.
    // findAll(Specification, Pageable) (used by searchTickets) is a different signature
    // and is NOT affected by this override.
    @Override
    @EntityGraph(attributePaths = {"category", "device"})
    List<Ticket> findAll(org.springframework.data.jpa.domain.Specification<Ticket> spec);

    // Analytics L1: ticket volume by day — returns [period String, count Long]
    @Query(value = "SELECT TO_CHAR(submitted_at, 'YYYY-MM-DD') AS period, COUNT(*) AS cnt " +
                   "FROM tickets " +
                   "WHERE submitted_at >= :startDate AND submitted_at <= :endDate " +
                   "GROUP BY TO_CHAR(submitted_at, 'YYYY-MM-DD') " +
                   "ORDER BY 1", nativeQuery = true)
    List<Object[]> countVolumeByDay(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L1: ticket volume by week — period = ISO week-start date
    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', submitted_at), 'YYYY-MM-DD') AS period, COUNT(*) AS cnt " +
                   "FROM tickets " +
                   "WHERE submitted_at >= :startDate AND submitted_at <= :endDate " +
                   "GROUP BY DATE_TRUNC('week', submitted_at) " +
                   "ORDER BY 1", nativeQuery = true)
    List<Object[]> countVolumeByWeek(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L1: ticket volume by month — period = "YYYY-MM"
    @Query(value = "SELECT TO_CHAR(submitted_at, 'YYYY-MM') AS period, COUNT(*) AS cnt " +
                   "FROM tickets " +
                   "WHERE submitted_at >= :startDate AND submitted_at <= :endDate " +
                   "GROUP BY TO_CHAR(submitted_at, 'YYYY-MM') " +
                   "ORDER BY 1", nativeQuery = true)
    List<Object[]> countVolumeByMonth(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L2: MTTR breakdown by category — returns [categoryId, categoryName, avgMttrMinutes, ticketCount]
    @Query(value = "SELECT c.id, c.name, " +
                   "AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.submitted_at)) / 60.0) AS avg_mttr, " +
                   "COUNT(t.id) AS ticket_count " +
                   "FROM tickets t " +
                   "JOIN categories c ON t.category_id = c.id " +
                   "WHERE t.resolved_at IS NOT NULL " +
                   "AND t.submitted_at >= :startDate AND t.submitted_at <= :endDate " +
                   "GROUP BY c.id, c.name " +
                   "ORDER BY c.name", nativeQuery = true)
    List<Object[]> getMttrByCategory(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L2: MTTR breakdown by technician — returns [technicianId, username, avgMttrMinutes, ticketCount]
    @Query(value = "SELECT t.assigned_technician_id, u.username, " +
                   "AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.submitted_at)) / 60.0) AS avg_mttr, " +
                   "COUNT(t.id) AS ticket_count " +
                   "FROM tickets t " +
                   "JOIN users u ON u.id = t.assigned_technician_id " +
                   "WHERE t.resolved_at IS NOT NULL AND t.assigned_technician_id IS NOT NULL " +
                   "AND t.submitted_at >= :startDate AND t.submitted_at <= :endDate " +
                   "GROUP BY t.assigned_technician_id, u.username " +
                   "ORDER BY u.username", nativeQuery = true)
    List<Object[]> getMttrByTechnician(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Analytics L2: MTTR breakdown by building — returns [buildingId, buildingName, avgMttrMinutes, ticketCount]
    @Query(value = "SELECT b.id, b.name, " +
                   "AVG(EXTRACT(EPOCH FROM (t.resolved_at - t.submitted_at)) / 60.0) AS avg_mttr, " +
                   "COUNT(t.id) AS ticket_count " +
                   "FROM tickets t " +
                   "JOIN devices d ON t.device_id = d.id " +
                   "JOIN rooms r ON d.room_id = r.id " +
                   "JOIN buildings b ON r.building_id = b.id " +
                   "WHERE t.resolved_at IS NOT NULL " +
                   "AND t.submitted_at >= :startDate AND t.submitted_at <= :endDate " +
                   "GROUP BY b.id, b.name " +
                   "ORDER BY b.name", nativeQuery = true)
    List<Object[]> getMttrByBuilding(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // searchTickets is replaced by TicketSpecification.withFilters() + findAll(spec, pageable)
    // — the old JPQL (:param IS NULL OR col=:param) pattern fails on PostgreSQL when null is
    //   bound for enum/timestamp parameters (SQLState 42P18: cannot determine type of $N).
}


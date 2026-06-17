package com.example.facility.dispatch.repository;

import com.example.facility.dispatch.model.DispatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DispatchHistoryRepository extends JpaRepository<DispatchHistory, Long> {

    List<DispatchHistory> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
    List<DispatchHistory> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    /**
     * Batch query: returns [technicianId, lastAssignedAt] for each technician that
     * appears in {@code ids}.  Used by the dispatch scoring engine to compute the
     * fairness factor (time since last assignment) without N individual queries.
     * Technicians with no history at all are simply absent from the result.
     */
    @Query("SELECT d.technicianId, MAX(d.createdAt) " +
           "FROM DispatchHistory d " +
           "WHERE d.technicianId IN :ids " +
           "GROUP BY d.technicianId")
    List<Object[]> findLastAssignmentTimesByTechnicianIds(@Param("ids") List<Long> ids);
}




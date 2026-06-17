package com.example.facility.ticket.repository;

import com.example.facility.ticket.model.ResolutionFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResolutionFeedbackRepository extends JpaRepository<ResolutionFeedback, Long> {

    Optional<ResolutionFeedback> findByTicketId(Long ticketId);

    List<ResolutionFeedback> findByTechnicianId(Long technicianId);

    // Returns [technicianId, avgRating] for batch quality scoring
    @Query("SELECT rf.technicianId, AVG(rf.rating) FROM ResolutionFeedback rf " +
           "WHERE rf.technicianId IN :ids GROUP BY rf.technicianId")
    List<Object[]> avgRatingByTechnicianIds(@Param("ids") List<Long> ids);

    @Query("SELECT AVG(rf.rating) FROM ResolutionFeedback rf WHERE rf.technicianId = :technicianId")
    Double avgRatingByTechnicianId(@Param("technicianId") Long technicianId);
}


package com.example.facility.ticket.repository;

import com.example.facility.ticket.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTicketIdOrderByCreatedAtDesc(Long ticketId);

    List<Comment> findByTicketId(Long ticketId);

    List<Comment> findByUserId(Long userId);
}


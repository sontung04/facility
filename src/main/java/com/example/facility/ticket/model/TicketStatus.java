package com.example.facility.ticket.model;

public enum TicketStatus {
    SUBMITTED,      // Requester tạo ticket
    ACK,            // Technician nhận thức
    ASSIGNED,       // Admin phân công cho technician
    IN_PROGRESS,    // Technician bắt đầu xử lý
    RESOLVED,       // Technician hoàn thành xử lý
    CLOSED          // Admin đóng ticket
}




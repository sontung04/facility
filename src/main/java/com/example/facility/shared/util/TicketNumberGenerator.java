package com.example.facility.shared.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class TicketNumberGenerator {
    
    @Value("${app.facility.ticket-prefix:TKT}")
    private String ticketPrefix;
    
    public String generateTicketNumber() {
        // Format: TKT-YYYYMMDDHHMSS-RANDOM
        LocalDateTime now = LocalDateTime.now();
        long random = System.nanoTime() % 10000;
        return String.format("%s-%04d%02d%02d%02d%02d%02d-%04d",
            ticketPrefix,
            now.getYear(),
            now.getMonthValue(),
            now.getDayOfMonth(),
            now.getHour(),
            now.getMinute(),
            now.getSecond(),
            random);
    }
}


package com.example.facility.ticket.service;

import com.example.facility.ticket.model.SeverityLevel;

/**
 * Stateless utility for mapping a numeric severity score to a {@link SeverityLevel} enum.
 * Kept inside the ticket module (ticket.service) because it directly references
 * {@code SeverityLevel} and is only ever called by {@link TicketService}.
 */
public class SeverityCalculator {

    private SeverityCalculator() {}

    /**
     * Maps a severity score to a {@link SeverityLevel}:
     * <ul>
     *   <li>LOW      [1.0, 2.0)</li>
     *   <li>MEDIUM   [2.0, 3.5)</li>
     *   <li>HIGH     [3.5, 4.5)</li>
     *   <li>CRITICAL [4.5, 5.0]</li>
     * </ul>
     */
    public static SeverityLevel calculateSeverityLevel(Float severityScore) {
        if (severityScore == null) {
            return SeverityLevel.LOW;
        }
        if (severityScore < 2.0f) {
            return SeverityLevel.LOW;
        } else if (severityScore < 3.5f) {
            return SeverityLevel.MEDIUM;
        } else if (severityScore < 4.5f) {
            return SeverityLevel.HIGH;
        } else {
            return SeverityLevel.CRITICAL;
        }
    }

    /**
     * Returns {@code true} if the severity level derived from {@code currentAvg}
     * is strictly higher than the one derived from {@code prevAvg}.
     */
    public static boolean isSeverityIncreased(Float prevAvg, Float currentAvg) {
        if (prevAvg == null || currentAvg == null) {
            return false;
        }
        SeverityLevel prevLevel    = calculateSeverityLevel(prevAvg);
        SeverityLevel currentLevel = calculateSeverityLevel(currentAvg);
        return currentLevel.ordinal() > prevLevel.ordinal();
    }
}

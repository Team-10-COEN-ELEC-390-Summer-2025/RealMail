package com.team10.realmail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatter {

    private static final ZoneId EASTERN_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a z");

    public static String formatDateForNotification(String dateString) {
        if (dateString == null || dateString.trim().isEmpty()) {
            return "Unknown time";
        }

        Instant instant = parseToInstant(dateString);
        if (instant != null) {
            return formatInstant(instant);
        }

        return dateString; // Return original if parsing fails
    }

    private static Instant parseToInstant(String dateString) {
        // Try parsing as ISO-8601 Instant
        try {
            return Instant.parse(dateString);
        } catch (DateTimeParseException e1) {
            // Try parsing as epoch milliseconds
            try {
                return Instant.ofEpochMilli(Long.parseLong(dateString));
            } catch (NumberFormatException e2) {
                // Add more parsing strategies if needed
                return null;
            }
        }
    }

    private static String formatInstant(Instant instant) {
        ZonedDateTime easternTime = instant.atZone(EASTERN_ZONE);
        return easternTime.format(FORMATTER);
    }
}

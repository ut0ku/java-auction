package ru.mirea.project.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Время системы хранится и сравнивается по московскому часовому поясу (МСК).
 * Europe/Moscow = UTC+3 круглый год (переход на летнее/зимнее время отменён с 2014 г.).
 */
public final class MoscowTime {
    public static final ZoneId ZONE = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private MoscowTime() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return dateTime.format(FORMATTER) + " МСК (UTC+3)";
    }
}

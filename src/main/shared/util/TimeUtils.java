package main.shared.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter CHART = DateTimeFormatter.ofPattern("HH:mm:ss");

    private TimeUtils() {
    }

    public static String display(LocalDateTime value) {
        return value == null ? "-" : value.format(DISPLAY);
    }

    public static String chartLabel(LocalDateTime value) {
        return value == null ? "-" : value.format(CHART);
    }
}

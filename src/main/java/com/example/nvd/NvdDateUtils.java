package com.example.nvd;

public final class NvdDateUtils {
    private NvdDateUtils() {}
    public static String localDateToNvdDate(java.time.LocalDate date) {
        return date.atStartOfDay(java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
    }
}

package com.googlesource.gerrit.plugins.chatgpt.utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class TimeUtils {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSSSSSSS";
    private static final ZoneOffset DEFAULT_ZONE_OFFSET = ZoneOffset.UTC;

    public static long getEpochSeconds(String updatedString) {
        LocalDateTime updatedDateTime = LocalDateTime.parse(updatedString, getFormatter());
        return updatedDateTime.toInstant(DEFAULT_ZONE_OFFSET).getEpochSecond();
    }

    public static String now() {
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE_OFFSET);
        return getFormatter().format(now);
    }

    public static long getCurrentMillis() {
        return System.currentTimeMillis();
    }

    private static DateTimeFormatter getFormatter() {
        return DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    }
}

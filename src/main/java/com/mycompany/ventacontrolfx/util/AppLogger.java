package com.mycompany.ventacontrolfx.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Basic Centralized Logging Utility.
 * Can be replaced by SLF4J/Log4j in real production environments.
 */
public class AppLogger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void error(String tag, String message, Throwable ex) {
        log("ERROR", tag, message);
        if (ex != null)
            ex.printStackTrace();
    }

    public static void info(String tag, String message) {
        log("INFO", tag, message);
    }

    public static void warn(String tag, String message) {
        log("WARN", tag, message);
    }

    private static void log(String level, String tag, String message) {
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] [%s] [%s]: %s%n", timestamp, level, tag, message);
    }
}

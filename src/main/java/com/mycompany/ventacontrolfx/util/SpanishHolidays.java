package com.mycompany.ventacontrolfx.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class SpanishHolidays {
    private static final Map<LocalDate, String> HOLIDAYS = new HashMap<>();

    static {
        // 2024
        HOLIDAYS.put(LocalDate.of(2024, 1, 1), "Año Nuevo");
        HOLIDAYS.put(LocalDate.of(2024, 1, 6), "Epifanía del Señor");
        HOLIDAYS.put(LocalDate.of(2024, 3, 29), "Viernes Santo");
        HOLIDAYS.put(LocalDate.of(2024, 5, 1), "Fiesta del Trabajo");
        HOLIDAYS.put(LocalDate.of(2024, 8, 15), "Asunción de la Virgen");
        HOLIDAYS.put(LocalDate.of(2024, 10, 12), "Fiesta Nacional de España");
        HOLIDAYS.put(LocalDate.of(2024, 11, 1), "Día de Todos los Santos");
        HOLIDAYS.put(LocalDate.of(2024, 12, 6), "Día de la Constitución Española");
        HOLIDAYS.put(LocalDate.of(2024, 12, 8), "Inmaculada Concepción");
        HOLIDAYS.put(LocalDate.of(2024, 12, 25), "Natividad del Señor");

        // 2025
        HOLIDAYS.put(LocalDate.of(2025, 1, 1), "Año Nuevo");
        HOLIDAYS.put(LocalDate.of(2025, 1, 6), "Epifanía del Señor");
        HOLIDAYS.put(LocalDate.of(2025, 4, 18), "Viernes Santo");
        HOLIDAYS.put(LocalDate.of(2025, 5, 1), "Fiesta del Trabajo");
        HOLIDAYS.put(LocalDate.of(2025, 8, 15), "Asunción de la Virgen");
        HOLIDAYS.put(LocalDate.of(2025, 10, 12), "Fiesta Nacional de España");
        HOLIDAYS.put(LocalDate.of(2025, 11, 1), "Día de Todos los Santos");
        HOLIDAYS.put(LocalDate.of(2025, 12, 6), "Día de la Constitución Española");
        HOLIDAYS.put(LocalDate.of(2025, 12, 8), "Inmaculada Concepción");
        HOLIDAYS.put(LocalDate.of(2025, 12, 25), "Natividad del Señor");

        // 2026
        HOLIDAYS.put(LocalDate.of(2026, 1, 1), "Año Nuevo");
        HOLIDAYS.put(LocalDate.of(2026, 1, 6), "Epifanía del Señor");
        HOLIDAYS.put(LocalDate.of(2026, 4, 3), "Viernes Santo");
        HOLIDAYS.put(LocalDate.of(2026, 5, 1), "Fiesta del Trabajo");
        HOLIDAYS.put(LocalDate.of(2026, 8, 15), "Asunción de la Virgen");
        HOLIDAYS.put(LocalDate.of(2026, 10, 12), "Fiesta Nacional de España");
        HOLIDAYS.put(LocalDate.of(2026, 11, 1), "Día de Todos los Santos");
        HOLIDAYS.put(LocalDate.of(2026, 12, 6), "Día de la Constitución Española");
        HOLIDAYS.put(LocalDate.of(2026, 12, 8), "Inmaculada Concepción");
        HOLIDAYS.put(LocalDate.of(2026, 12, 25), "Natividad del Señor");
    }

    public static String getHolidayName(LocalDate date) {
        return HOLIDAYS.get(date);
    }

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS.containsKey(date);
    }
}

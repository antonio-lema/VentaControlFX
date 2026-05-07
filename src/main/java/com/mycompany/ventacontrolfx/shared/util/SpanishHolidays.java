package com.mycompany.ventacontrolfx.shared.util;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class SpanishHolidays {
    private static final Map<LocalDate, String> HOLIDAYS = new HashMap<>();

    static {
        // 2024
        HOLIDAYS.put(LocalDate.of(2024, 1, 1), "holiday.new_year");
        HOLIDAYS.put(LocalDate.of(2024, 1, 6), "holiday.epiphany");
        HOLIDAYS.put(LocalDate.of(2024, 3, 29), "holiday.good_friday");
        HOLIDAYS.put(LocalDate.of(2024, 5, 1), "holiday.labor_day");
        HOLIDAYS.put(LocalDate.of(2024, 8, 15), "holiday.assumption");
        HOLIDAYS.put(LocalDate.of(2024, 10, 12), "holiday.national_day");
        HOLIDAYS.put(LocalDate.of(2024, 11, 1), "holiday.all_saints");
        HOLIDAYS.put(LocalDate.of(2024, 12, 6), "holiday.constitution");
        HOLIDAYS.put(LocalDate.of(2024, 12, 8), "holiday.immaculate");
        HOLIDAYS.put(LocalDate.of(2024, 12, 25), "holiday.christmas");

        // 2025
        HOLIDAYS.put(LocalDate.of(2025, 1, 1), "holiday.new_year");
        HOLIDAYS.put(LocalDate.of(2025, 1, 6), "holiday.epiphany");
        HOLIDAYS.put(LocalDate.of(2025, 4, 18), "holiday.good_friday");
        HOLIDAYS.put(LocalDate.of(2025, 5, 1), "holiday.labor_day");
        HOLIDAYS.put(LocalDate.of(2025, 8, 15), "holiday.assumption");
        HOLIDAYS.put(LocalDate.of(2025, 10, 12), "holiday.national_day");
        HOLIDAYS.put(LocalDate.of(2025, 11, 1), "holiday.all_saints");
        HOLIDAYS.put(LocalDate.of(2025, 12, 6), "holiday.constitution");
        HOLIDAYS.put(LocalDate.of(2025, 12, 8), "holiday.immaculate");
        HOLIDAYS.put(LocalDate.of(2025, 12, 25), "holiday.christmas");

        // 2026
        HOLIDAYS.put(LocalDate.of(2026, 1, 1), "holiday.new_year");
        HOLIDAYS.put(LocalDate.of(2026, 1, 6), "holiday.epiphany");
        HOLIDAYS.put(LocalDate.of(2026, 4, 3), "holiday.good_friday");
        HOLIDAYS.put(LocalDate.of(2026, 5, 1), "holiday.labor_day");
        HOLIDAYS.put(LocalDate.of(2026, 8, 15), "holiday.assumption");
        HOLIDAYS.put(LocalDate.of(2026, 10, 12), "holiday.national_day");
        HOLIDAYS.put(LocalDate.of(2026, 11, 1), "holiday.all_saints");
        HOLIDAYS.put(LocalDate.of(2026, 12, 6), "holiday.constitution");
        HOLIDAYS.put(LocalDate.of(2026, 12, 8), "holiday.immaculate");
        HOLIDAYS.put(LocalDate.of(2026, 12, 25), "holiday.christmas");
    }

    public static String getHolidayName(LocalDate date) {
        return HOLIDAYS.get(date);
    }

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS.containsKey(date);
    }
}


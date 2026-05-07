package com.mycompany.ventacontrolfx.shared.util;

/**
 * Utility class for common data validations.
 */
public class ValidationUtil {

    /**
     * Checks if a string is null or empty (after trimming).
     */
    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Basic check to see if a string looks like an email.
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email))
            return false;
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Checks if a string is a valid positive number.
     */
    public static boolean isPositiveNumber(String text) {
        if (isEmpty(text))
            return false;
        try {
            double value = Double.parseDouble(text);
            return value >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}


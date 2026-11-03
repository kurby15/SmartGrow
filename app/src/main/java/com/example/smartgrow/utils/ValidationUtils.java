package com.example.smartgrow.utils;

import com.example.smartgrow.core.SecurityLogger;
import java.util.regex.Pattern;

public class ValidationUtils {

    // Prevent SQL-like characters and script tags
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[<>{}\\[\\]\\\\$|]");

    /**
     * Sanitizes input by removing potentially harmful characters for search or diary notes.
     */
    public static String sanitize(String input) {
        if (input == null) return "";
        
        if (SANITIZE_PATTERN.matcher(input).find()) {
            SecurityLogger.log(SecurityLogger.SecurityEvent.INPUT_VALIDATION_FAILURE, 
                "Potential malicious characters detected and sanitized in input: " + input);
        }

        // Remove common injection characters
        String sanitized = SANITIZE_PATTERN.matcher(input).replaceAll("");
        
        // Trim and prevent excessive length (e.g., 500 chars for notes)
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 500);
        }
        return sanitized.trim();
    }

    /**
     * Validates if a search query is safe and not just special characters.
     */
    public static boolean isValidSearch(String query) {
        if (query == null || query.trim().isEmpty()) return false;
        
        boolean isValid = query.matches(".*[a-zA-Z0-9].*");
        if (!isValid) {
            SecurityLogger.log(SecurityLogger.SecurityEvent.INPUT_VALIDATION_FAILURE, 
                "Invalid search query (no alphanumeric characters): " + query);
        }
        return isValid;
    }
}

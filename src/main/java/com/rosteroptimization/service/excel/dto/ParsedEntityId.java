
package com.rosteroptimization.service.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

/**
 * Helper class for parsing entity IDs from Excel cells
 * Supports formats: "name_id", "name", "id"
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedEntityId {

    private String name;      // Entity name (for lookup)
    private Long id;          // Entity ID (direct reference)
    private String rawValue;  // Original value from Excel
    private ParseType type;   // How the value was parsed

    /**
     * Parse entity ID from string value
     */
    public static ParsedEntityId parse(String value) {
        if (!StringUtils.hasText(value)) {
            return new ParsedEntityId(null, null, value, ParseType.EMPTY);
        }

        String trimmedValue = value.trim();

        // Format 1: "name_id" (e.g., "Department1_5")
        if (trimmedValue.contains("_")) {
            String[] parts = trimmedValue.split("_");
            if (parts.length == 2) {
                String namePart = parts[0].trim();
                String idPart = parts[1].trim();

                try {
                    Long id = Long.parseLong(idPart);
                    return new ParsedEntityId(namePart, id, trimmedValue, ParseType.NAME_ID);
                } catch (NumberFormatException e) {
                    // Invalid ID part, treat as name only
                    return new ParsedEntityId(trimmedValue, null, trimmedValue, ParseType.NAME_ONLY);
                }
            } else {
                // Multiple underscores, treat as name
                return new ParsedEntityId(trimmedValue, null, trimmedValue, ParseType.NAME_ONLY);
            }
        }

        // Format 2: Pure number (e.g., "5")
        try {
            Long id = Long.parseLong(trimmedValue);
            return new ParsedEntityId(null, id, trimmedValue, ParseType.ID_ONLY);
        } catch (NumberFormatException e) {
            // Not a number
        }

        // Format 3: Name only (e.g., "Department1")
        return new ParsedEntityId(trimmedValue, null, trimmedValue, ParseType.NAME_ONLY);
    }

    /**
     * Check if parsed value has name
     */
    public boolean hasName() {
        return StringUtils.hasText(name);
    }

    /**
     * Check if parsed value has ID
     */
    public boolean hasId() {
        return id != null && id > 0;
    }

    /**
     * Check if value is empty
     */
    public boolean isEmpty() {
        return type == ParseType.EMPTY;
    }

    /**
     * Check if this is a valid reference (has either name or ID)
     */
    public boolean isValid() {
        return hasName() || hasId();
    }

    /**
     * Get display value for UI
     */
    public String getDisplayValue() {
        switch (type) {
            case NAME_ID:
                return name + " (ID: " + id + ")";
            case NAME_ONLY:
                return name;
            case ID_ONLY:
                return "ID: " + id;
            case EMPTY:
                return "(empty)";
            default:
                return rawValue;
        }
    }

    /**
     * Get lookup strategy description
     */
    public String getLookupStrategy() {
        switch (type) {
            case NAME_ID:
                return "Lookup by name '" + name + "', verify ID " + id;
            case NAME_ONLY:
                return "Lookup by name '" + name + "'";
            case ID_ONLY:
                return "Lookup by ID " + id;
            case EMPTY:
                return "No lookup needed (empty)";
            default:
                return "Unknown strategy";
        }
    }

    /**
     * Enum for parse types
     */
    public enum ParseType {
        NAME_ID,    // "name_id" format
        NAME_ONLY,  // Just name
        ID_ONLY,    // Just numeric ID
        EMPTY       // Empty or null value
    }
}
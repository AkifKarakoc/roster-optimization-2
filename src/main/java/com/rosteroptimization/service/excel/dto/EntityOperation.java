package com.rosteroptimization.service.excel.dto;

import org.springframework.util.StringUtils;

/**
 * Enum for Excel entity operations
 */
public enum EntityOperation {

    ADD("Add", "Create new entity"),
    UPDATE("Update", "Update existing entity"),
    DELETE("Delete", "Delete existing entity");

    private final String displayName;
    private final String description;

    EntityOperation(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse operation from string (case-insensitive)
     */
    public static EntityOperation fromString(String operationStr) {
        if (!StringUtils.hasText(operationStr)) {
            return ADD; // Default
        }

        String normalized = operationStr.trim().toUpperCase();

        // Handle various formats
        switch (normalized) {
            case "ADD":
            case "CREATE":
            case "INSERT":
            case "NEW":
                return ADD;

            case "UPDATE":
            case "MODIFY":
            case "EDIT":
            case "CHANGE":
                return UPDATE;

            case "DELETE":
            case "REMOVE":
            case "DEL":
                return DELETE;

            default:
                throw new IllegalArgumentException("Unknown operation: " + operationStr);
        }
    }

    /**
     * Check if operation requires existing entity ID
     */
    public boolean requiresExistingEntity() {
        return this == UPDATE || this == DELETE;
    }

    /**
     * Check if operation requires full data
     */
    public boolean requiresFullData() {
        return this == ADD || this == UPDATE;
    }

    /**
     * Get valid operation values for display/help
     */
    public static String getValidValues() {
        return "ADD/CREATE/INSERT/NEW, UPDATE/MODIFY/EDIT/CHANGE, DELETE/REMOVE/DEL";
    }
}
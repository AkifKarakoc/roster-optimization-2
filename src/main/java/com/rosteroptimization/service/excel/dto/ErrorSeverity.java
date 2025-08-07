package com.rosteroptimization.service.excel.dto;

/**
 * Enum for error severity levels in Excel validation
 */
public enum ErrorSeverity {

    /**
     * Critical error - Row cannot be imported
     */
    ERROR,

    /**
     * Warning - Row can be imported but should be reviewed
     */
    WARNING,

    /**
     * Informational - Just for user awareness
     */
    INFO;

    /**
     * Check if this severity blocks import
     */
    public boolean isBlocking() {
        return this == ERROR;
    }

    /**
     * Get display color for UI
     */
    public String getDisplayColor() {
        switch (this) {
            case ERROR: return "red";
            case WARNING: return "orange";
            case INFO: return "blue";
            default: return "gray";
        }
    }

    /**
     * Get display text
     */
    public String getDisplayText() {
        switch (this) {
            case ERROR: return "Error";
            case WARNING: return "Warning";
            case INFO: return "Info";
            default: return "Unknown";
        }
    }
}
package com.rosteroptimization.service.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a validation error for an Excel row/field
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelErrorDTO {

    private String fieldName; // Column name where error occurred
    private ErrorSeverity severity; // ERROR, WARNING, INFO
    private String errorMessage; // Human-readable error message
    private String suggestedFix; // Optional suggestion for fixing the error
    private String currentValue; // Current value that caused the error
    private String expectedFormat; // Expected format/value

    /**
     * Create error with basic info
     */
    public static ExcelErrorDTO error(String fieldName, String message) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage(message)
                .build();
    }

    /**
     * Create warning with basic info
     */
    public static ExcelErrorDTO warning(String fieldName, String message) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.WARNING)
                .errorMessage(message)
                .build();
    }

    /**
     * Create info with basic info
     */
    public static ExcelErrorDTO info(String fieldName, String message) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.INFO)
                .errorMessage(message)
                .build();
    }

    /**
     * Create error with suggestion
     */
    public static ExcelErrorDTO errorWithSuggestion(String fieldName, String message, String suggestion) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage(message)
                .suggestedFix(suggestion)
                .build();
    }

    /**
     * Create format error with expected format
     */
    public static ExcelErrorDTO formatError(String fieldName, String currentValue, String expectedFormat) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage("Invalid format for field: " + fieldName)
                .currentValue(currentValue)
                .expectedFormat(expectedFormat)
                .suggestedFix("Expected format: " + expectedFormat)
                .build();
    }

    /**
     * Create required field error
     */
    public static ExcelErrorDTO requiredFieldError(String fieldName) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage("Required field is missing or empty")
                .suggestedFix("Please provide a value for " + fieldName)
                .build();
    }

    /**
     * Create foreign key not found error
     */
    public static ExcelErrorDTO foreignKeyError(String fieldName, String entityName, String keyValue) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage(entityName + " not found: " + keyValue)
                .currentValue(keyValue)
                .suggestedFix("Check if " + entityName + " exists and is active")
                .build();
    }

    /**
     * Create duplicate value error
     */
    public static ExcelErrorDTO duplicateError(String fieldName, String value) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.ERROR)
                .errorMessage("Duplicate value: " + value)
                .currentValue(value)
                .suggestedFix("Use a unique value for " + fieldName)
                .build();
    }

    /**
     * Create entity not found error (for UPDATE/DELETE operations)
     */
    public static ExcelErrorDTO entityNotFoundError(String fieldName, String entityType, String idValue) {
        return ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(ErrorSeverity.WARNING) // Warning, not error - can skip this row
                .errorMessage(entityType + " not found for UPDATE/DELETE: " + idValue)
                .currentValue(idValue)
                .suggestedFix("Check if " + entityType + " exists with ID: " + idValue)
                .build();
    }

    /**
     * Check if this error blocks import
     */
    public boolean isBlocking() {
        return severity == ErrorSeverity.ERROR;
    }

    /**
     * Get full error description for display
     */
    public String getFullDescription() {
        StringBuilder desc = new StringBuilder();
        desc.append("[").append(severity.getDisplayText()).append("] ");

        if (fieldName != null) {
            desc.append(fieldName).append(": ");
        }

        desc.append(errorMessage);

        if (currentValue != null) {
            desc.append(" (Current: ").append(currentValue).append(")");
        }

        if (suggestedFix != null) {
            desc.append(" - ").append(suggestedFix);
        }

        return desc.toString();
    }
}
package com.rosteroptimization.service.excel.dto;

import com.rosteroptimization.service.excel.ExcelEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a single Excel row with validation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelRowDTO {

    private String rowId; // Unique identifier for this row
    private int rowNumber; // Excel row number (1-based)
    private ExcelEntityType entityType;
    private Map<String, String> cellValues;
    private List<ExcelErrorDTO> errors;
    private boolean canImport; // True if row can be imported (no blocking errors)

    // Processing info
    private EntityOperation operation; // ADD, UPDATE, DELETE
    private Long entityId; // Parsed entity ID (for UPDATE/DELETE)
    private String entityName; // Parsed entity name

    /**
     * Get cell value by column name
     */
    public String getCellValue(String columnName) {
        if (cellValues == null || columnName == null) return "";
        return cellValues.getOrDefault(columnName.toUpperCase(), "");
    }

    /**
     * Check if cell has value
     */
    public boolean hasCellValue(String columnName) {
        return StringUtils.hasText(getCellValue(columnName));
    }

    /**
     * Set cell value
     */
    public void setCellValue(String columnName, String value) {
        if (cellValues != null && columnName != null) {
            cellValues.put(columnName.toUpperCase(), value != null ? value : "");
        }
    }

    /**
     * Add validation error
     */
    public void addError(ExcelErrorDTO error) {
        if (errors != null) {
            errors.add(error);
            // Update canImport flag based on error severity
            updateCanImportFlag();
        }
    }

    /**
     * Add validation error with parameters
     */
    public void addError(String fieldName, ErrorSeverity severity, String message) {
        addError(ExcelErrorDTO.builder()
                .fieldName(fieldName)
                .severity(severity)
                .errorMessage(message)
                .build());
    }

    /**
     * Check if row has any errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if row has blocking errors (ERROR severity)
     */
    public boolean hasBlockingErrors() {
        if (errors == null) return false;

        return errors.stream()
                .anyMatch(error -> error.getSeverity() == ErrorSeverity.ERROR);
    }

    /**
     * Check if row has warnings (WARNING severity)
     */
    public boolean hasWarnings() {
        if (errors == null) return false;

        return errors.stream()
                .anyMatch(error -> error.getSeverity() == ErrorSeverity.WARNING);
    }

    /**
     * Get operation from cell values
     */
    public EntityOperation getOperationFromCell() {
        // Check common operation column names
        String[] operationColumns = {"OPERATION", "OP", "ACTION"};

        for (String column : operationColumns) {
            String operationValue = getCellValue(column);
            if (StringUtils.hasText(operationValue)) {
                try {
                    return EntityOperation.fromString(operationValue);
                } catch (IllegalArgumentException e) {
                    // Invalid operation value, will be handled in validation
                    return EntityOperation.ADD; // Default
                }
            }
        }

        return EntityOperation.ADD; // Default if no operation column
    }

    /**
     * Get main entity ID from appropriate column based on entity type
     */
    public String getMainEntityId() {
        if (cellValues == null || cellValues.isEmpty() || entityType == null) return "";

        String mainEntityColumn = getMainEntityColumnName();
        return getCellValue(mainEntityColumn);
    }

    /**
     * Get the main entity column name based on entity type
     */
    private String getMainEntityColumnName() {
        if (entityType == null) {
            // Fallback to first column if entity type is not set
            return cellValues.keySet().iterator().next();
        }

        switch (entityType) {
            case DEPARTMENT:
                return "DEPARTMENT ID";
            case QUALIFICATION:
                return "QUALIFICATION ID";
            case WORKING_PERIOD:
                return "PERIOD ID";
            case SQUAD_WORKING_PATTERN:
                return "PATTERN ID";
            case SQUAD:
                return "SQUAD ID";
            case SHIFT:
                return "SHIFT ID";
            case STAFF:
                return "STAFF ID";
            case TASK:
                return "TASK ID";
            default:
                // Fallback to first column for unknown types
                return cellValues.keySet().iterator().next();
        }
    }

    /**
     * Parse entity ID and name from main ID column
     */
    public ParsedEntityId parseMainEntityId() {
        String mainId = getMainEntityId();
        return ParsedEntityId.parse(mainId);
    }

    /**
     * Update canImport flag based on current errors
     */
    private void updateCanImportFlag() {
        this.canImport = !hasBlockingErrors();
    }

    /**
     * Get error count by severity
     */
    public int getErrorCount(ErrorSeverity severity) {
        if (errors == null) return 0;

        return (int) errors.stream()
                .filter(error -> error.getSeverity() == severity)
                .count();
    }

    /**
     * Get summary of errors for display
     */
    public String getErrorSummary() {
        if (!hasErrors()) return "No errors";

        int errorCount = getErrorCount(ErrorSeverity.ERROR);
        int warningCount = getErrorCount(ErrorSeverity.WARNING);

        StringBuilder summary = new StringBuilder();
        if (errorCount > 0) {
            summary.append(errorCount).append(" error(s)");
        }
        if (warningCount > 0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(warningCount).append(" warning(s)");
        }

        return summary.toString();
    }
}
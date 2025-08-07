package com.rosteroptimization.service.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for import operation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDTO {

    private String sessionId; // Original session ID
    private LocalDateTime importStartedAt;
    private LocalDateTime importCompletedAt;
    private long executionTimeMs;

    // Overall statistics
    private int totalRowsProcessed;
    private int successCount;
    private int failureCount;
    private int skippedCount;

    // Detailed results by entity type
    private Map<String, EntityImportResult> entityResults; // Key: EntityType name

    // Processing details
    private List<ImportOperationDTO> operations; // Individual operations performed
    private List<ImportErrorDTO> errors; // All errors that occurred
    private List<String> warnings; // General warnings
    private List<String> infoMessages; // Informational messages

    // Final status
    private ImportStatus status;
    private String statusMessage;
    private boolean hasFailures;
    private boolean partialSuccess; // Some succeeded, some failed

    /**
     * Calculate and update statistics
     */
    public void updateStatistics() {
        if (operations != null) {
            this.totalRowsProcessed = operations.size();
            this.successCount = (int) operations.stream().filter(op -> op.isSuccess()).count();
            this.failureCount = (int) operations.stream().filter(op -> !op.isSuccess()).count();
            this.skippedCount = totalRowsProcessed - successCount - failureCount;
        }

        this.hasFailures = failureCount > 0;
        this.partialSuccess = successCount > 0 && failureCount > 0;

        // Determine overall status
        if (failureCount == 0) {
            this.status = ImportStatus.SUCCESS;
            this.statusMessage = "All rows imported successfully";
        } else if (successCount == 0) {
            this.status = ImportStatus.FAILURE;
            this.statusMessage = "All rows failed to import";
        } else {
            this.status = ImportStatus.PARTIAL_SUCCESS;
            this.statusMessage = String.format("%d succeeded, %d failed", successCount, failureCount);
        }

        // Calculate execution time
        if (importStartedAt != null && importCompletedAt != null) {
            this.executionTimeMs = java.time.Duration.between(importStartedAt, importCompletedAt).toMillis();
        }
    }

    /**
     * Add operation result
     */
    public void addOperation(ImportOperationDTO operation) {
        if (operations != null) {
            operations.add(operation);
        }
    }

    /**
     * Add error
     */
    public void addError(ImportErrorDTO error) {
        if (errors != null) {
            errors.add(error);
        }
    }

    /**
     * Add warning
     */
    public void addWarning(String warning) {
        if (warnings != null) {
            warnings.add(warning);
        }
    }

    /**
     * Add info message
     */
    public void addInfo(String message) {
        if (infoMessages != null) {
            infoMessages.add(message);
        }
    }

    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        if (totalRowsProcessed == 0) return 0.0;
        return (double) successCount / totalRowsProcessed * 100.0;
    }

    /**
     * Get execution time in seconds
     */
    public double getExecutionTimeSeconds() {
        return executionTimeMs / 1000.0;
    }

    /**
     * Get summary for display
     */
    public String getSummary() {
        return String.format("Processed: %d, Success: %d, Failed: %d, Time: %.2fs",
                totalRowsProcessed, successCount, failureCount, getExecutionTimeSeconds());
    }

    /**
     * Entity-specific import result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EntityImportResult {
        private String entityType;
        private int processed;
        private int succeeded;
        private int failed;
        private int skipped;
        private List<String> errors;
        private List<String> warnings;

        public double getSuccessRate() {
            if (processed == 0) return 0.0;
            return (double) succeeded / processed * 100.0;
        }
    }

    /**
     * Individual import operation result
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportOperationDTO {
        private String rowId;
        private String entityType;
        private EntityOperation operation; // ADD, UPDATE, DELETE
        private boolean success;
        private String errorMessage;
        private Long entityId; // ID of created/updated entity
        private LocalDateTime processedAt;

        public boolean isSuccess() {
            return success;
        }
    }

    /**
     * Import error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportErrorDTO {
        private String rowId;
        private String entityType;
        private EntityOperation operation;
        private String errorMessage;
        private String errorDetails; // Stack trace or detailed error
        private ErrorSeverity severity;
        private LocalDateTime occurredAt;

        public boolean isBlocking() {
            return severity == ErrorSeverity.ERROR;
        }
    }

    /**
     * Overall import status
     */
    public enum ImportStatus {
        SUCCESS,          // All rows imported successfully
        PARTIAL_SUCCESS,  // Some rows succeeded, some failed
        FAILURE,          // All rows failed
        CANCELLED         // Import was cancelled by user
    }
}
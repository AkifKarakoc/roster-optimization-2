package com.rosteroptimization.service.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * DTO for import request containing selected rows to import
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportRequestDTO {

    @NotEmpty(message = "At least one row must be selected for import")
    private List<String> selectedRowIds; // List of row IDs to import

    private boolean continueOnError; // Continue import even if some entities fail
    private boolean validateBeforeImport; // Re-validate before import (recommended)

    // Import options
    private ImportOptions options;

    /**
     * Get count of selected rows
     */
    public int getSelectedCount() {
        return selectedRowIds != null ? selectedRowIds.size() : 0;
    }

    /**
     * Check if row is selected
     */
    public boolean isRowSelected(String rowId) {
        return selectedRowIds != null && selectedRowIds.contains(rowId);
    }

    /**
     * Import options nested class
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportOptions {

        // Transaction management
        private boolean useTransactions; // Use database transactions (recommended)
        private boolean rollbackOnError; // Rollback entire batch on any error

        // Conflict resolution
        private ConflictResolution conflictResolution; // How to handle conflicts
        private boolean skipDuplicates; // Skip rows with duplicate keys
        private boolean updateExisting; // Update existing entities instead of error

        // Performance settings
        private int batchSize; // Batch size for bulk operations (default: 100)
        private boolean enableParallelProcessing; // Process entities in parallel

        // Logging and reporting
        private boolean detailedLogging; // Enable detailed operation logging
        private boolean generateReport; // Generate import report file

        /**
         * Get default import options
         */
        public static ImportOptions getDefault() {
            return ImportOptions.builder()
                    .useTransactions(true)
                    .rollbackOnError(false) // Continue with other batches
                    .conflictResolution(ConflictResolution.SKIP)
                    .skipDuplicates(true)
                    .updateExisting(false)
                    .batchSize(100)
                    .enableParallelProcessing(false) // Keep simple for now
                    .detailedLogging(true)
                    .generateReport(true)
                    .build();
        }
    }

    /**
     * Conflict resolution strategies
     */
    public enum ConflictResolution {
        SKIP,           // Skip conflicting row, continue with next
        ERROR,          // Throw error and stop processing
        UPDATE,         // Try to update existing entity
        CREATE_NEW      // Create with modified key (if possible)
    }

    /**
     * Get default import request
     */
    public static ImportRequestDTO createDefault(List<String> selectedRowIds) {
        return ImportRequestDTO.builder()
                .selectedRowIds(selectedRowIds)
                .continueOnError(true) // Default: continue on error
                .validateBeforeImport(true) // Default: re-validate
                .options(ImportOptions.getDefault())
                .build();
    }

    /**
     * Validate import request
     */
    public void validate() {
        if (selectedRowIds == null || selectedRowIds.isEmpty()) {
            throw new IllegalArgumentException("No rows selected for import");
        }

        // Set default options if not provided
        if (options == null) {
            options = ImportOptions.getDefault();
        }

        // Validate batch size
        if (options.getBatchSize() <= 0) {
            options.setBatchSize(100); // Default batch size
        }
    }
}
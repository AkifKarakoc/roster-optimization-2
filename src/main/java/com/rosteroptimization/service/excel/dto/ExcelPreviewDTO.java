package com.rosteroptimization.service.excel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Excel preview response containing all sheets and validation results
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelPreviewDTO {

    private String sessionId; // Unique session identifier
    private String fileName; // Original Excel file name
    private LocalDateTime processedAt; // When the file was processed
    private List<ExcelSheetDTO> sheets; // All parsed sheets

    // Overall statistics
    private int totalSheets;
    private int totalRows;
    private int validRows;
    private int errorRows;
    private int warningRows;
    private int importableRows; // Rows that can actually be imported

    // Processing summary
    private Map<String, Integer> entityTypeCounts; // Count per entity type
    private List<String> processingMessages; // General messages/warnings
    private boolean hasBlockingErrors; // True if any sheet has blocking errors
    private long processingTimeMs; // Processing time in milliseconds

    /**
     * Update overall statistics from sheets
     */
    public void updateStatistics() {
        if (sheets == null) {
            resetStatistics();
            return;
        }

        this.totalSheets = sheets.size();
        this.totalRows = sheets.stream().mapToInt(ExcelSheetDTO::getTotalRows).sum();
        this.validRows = sheets.stream().mapToInt(ExcelSheetDTO::getValidRows).sum();
        this.errorRows = sheets.stream().mapToInt(ExcelSheetDTO::getErrorRows).sum();
        this.warningRows = sheets.stream().mapToInt(ExcelSheetDTO::getWarningRows).sum();
        this.importableRows = sheets.stream().mapToInt(ExcelSheetDTO::getImportableRows).sum();

        // Check for blocking errors
        this.hasBlockingErrors = sheets.stream().anyMatch(sheet -> sheet.getErrorRows() > 0);

        // Update entity type counts
        if (entityTypeCounts != null) {
            entityTypeCounts.clear();
            for (ExcelSheetDTO sheet : sheets) {
                if (sheet.getEntityType() != null) {
                    String entityType = sheet.getEntityType().name();
                    entityTypeCounts.merge(entityType, sheet.getTotalRows(), Integer::sum);
                }
            }
        }
    }

    /**
     * Reset all statistics to zero
     */
    private void resetStatistics() {
        this.totalSheets = 0;
        this.totalRows = 0;
        this.validRows = 0;
        this.errorRows = 0;
        this.warningRows = 0;
        this.importableRows = 0;
        this.hasBlockingErrors = false;
    }

    /**
     * Get sheet by entity type
     */
    public ExcelSheetDTO getSheetByEntityType(String entityTypeName) {
        if (sheets == null || entityTypeName == null) return null;

        return sheets.stream()
                .filter(sheet -> sheet.getEntityType() != null &&
                        sheet.getEntityType().name().equalsIgnoreCase(entityTypeName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if preview has any data to import
     */
    public boolean hasImportableData() {
        return importableRows > 0;
    }

    /**
     * Get success rate percentage
     */
    public double getSuccessRate() {
        if (totalRows == 0) return 0.0;
        return (double) validRows / totalRows * 100.0;
    }

    /**
     * Get import readiness summary for UI
     */
    public String getImportReadinessSummary() {
        if (totalRows == 0) {
            return "No data found";
        }

        if (hasBlockingErrors) {
            return String.format("%d rows ready, %d blocked by errors", importableRows, errorRows);
        } else if (warningRows > 0) {
            return String.format("%d rows ready, %d with warnings", validRows, warningRows);
        } else {
            return String.format("All %d rows ready for import", totalRows);
        }
    }

    /**
     * Get processing summary for display
     */
    public ProcessingSummaryDTO getProcessingSummary() {
        return ProcessingSummaryDTO.builder()
                .fileName(fileName)
                .processedAt(processedAt)
                .totalSheets(totalSheets)
                .totalRows(totalRows)
                .successRate(getSuccessRate())
                .importableRows(importableRows)
                .hasBlockingErrors(hasBlockingErrors)
                .readinessSummary(getImportReadinessSummary())
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * Inner DTO for processing summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessingSummaryDTO {
        private String fileName;
        private LocalDateTime processedAt;
        private int totalSheets;
        private int totalRows;
        private double successRate;
        private int importableRows;
        private boolean hasBlockingErrors;
        private String readinessSummary;
        private long processingTimeMs;
    }
}
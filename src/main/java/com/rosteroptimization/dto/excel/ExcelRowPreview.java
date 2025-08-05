package com.rosteroptimization.dto.excel;

import com.rosteroptimization.enums.RowImportStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Enhanced version of ExcelRowResult with detailed cell-level information
 * and import status tracking
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExcelRowPreview<T> extends ExcelRowResult<T> {
    
    /**
     * Detailed information for each cell in the row
     * Key: column name, Value: cell details
     */
    private Map<String, CellDetail> cellDetails;
    
    /**
     * Import status of this row
     */
    private RowImportStatus importStatus;
    
    /**
     * Error message if import failed
     */
    private String importErrorMessage;
    
    /**
     * Timestamp when the row was imported
     */
    private LocalDateTime importedAt;
    
    /**
     * Whether this row is selected for import
     */
    private boolean selected;
    
    /**
     * Overall row status based on cell statuses
     */
    private String rowStatus; // "valid", "warning", "error"
    
    /**
     * Entity type this row belongs to
     */
    private String entityType;

    public ExcelRowPreview() {
        super();
        this.cellDetails = new HashMap<>();
        this.importStatus = RowImportStatus.NOT_PROCESSED;
        this.selected = true; // Default to selected if valid
    }
    
    /**
     * Add cell detail information
     */
    public void addCellDetail(String columnName, CellDetail cellDetail) {
        this.cellDetails.put(columnName, cellDetail);
        updateRowStatus();
    }
    
    /**
     * Update overall row status based on cell statuses
     */
    private void updateRowStatus() {
        boolean hasError = cellDetails.values().stream()
                .anyMatch(cell -> cell.getStatus() == com.rosteroptimization.enums.CellStatus.ERROR);
        boolean hasWarning = cellDetails.values().stream()
                .anyMatch(cell -> cell.getStatus() == com.rosteroptimization.enums.CellStatus.WARNING);
        
        if (hasError) {
            this.rowStatus = "error";
            this.selected = false; // Don't select rows with errors
            this.setValid(false);
        } else if (hasWarning) {
            this.rowStatus = "warning";
        } else {
            this.rowStatus = "valid";
        }
    }
    
    /**
     * Mark row as imported successfully
     */
    public void markAsImported() {
        this.importStatus = RowImportStatus.IMPORTED;
        this.importedAt = LocalDateTime.now();
        this.importErrorMessage = null;
    }
    
    /**
     * Mark row as failed during import
     */
    public void markAsFailed(String errorMessage) {
        this.importStatus = RowImportStatus.FAILED;
        this.importErrorMessage = errorMessage;
        this.importedAt = LocalDateTime.now();
    }
    
    /**
     * Mark row as skipped (not selected)
     */
    public void markAsSkipped() {
        this.importStatus = RowImportStatus.SKIPPED;
    }
}

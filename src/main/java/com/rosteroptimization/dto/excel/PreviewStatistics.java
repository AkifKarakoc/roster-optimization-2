package com.rosteroptimization.dto.excel;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Detailed statistics for Excel preview
 */
@Data
public class PreviewStatistics {
    /**
     * Total number of rows in the Excel file
     */
    private int totalRows;
    
    /**
     * Number of rows with all valid cells
     */
    private int validRows;
    
    /**
     * Number of rows with at least one error cell
     */
    private int errorRows;
    
    /**
     * Number of rows with warnings but no errors
     */
    private int warningRows;
    
    /**
     * Number of rows selected for import
     */
    private int selectedRows;
    
    /**
     * Count of operations by type (ADD: 5, UPDATE: 3, DELETE: 1)
     */
    private Map<String, Integer> operationCounts;
    
    /**
     * Count of errors by field name (NAME: 3, EMAIL: 2, etc.)
     */
    private Map<String, Integer> errorCounts;
    
    /**
     * Count of warnings by field name
     */
    private Map<String, Integer> warningCounts;
    
    /**
     * Count of cell statuses by type
     */
    private Map<String, Integer> cellStatusCounts;
    
    public PreviewStatistics() {
        this.operationCounts = new HashMap<>();
        this.errorCounts = new HashMap<>();
        this.warningCounts = new HashMap<>();
        this.cellStatusCounts = new HashMap<>();
    }
    
    /**
     * Calculate percentage of valid rows
     */
    public double getValidPercentage() {
        if (totalRows == 0) return 0.0;
        return (double) validRows / totalRows * 100.0;
    }
    
    /**
     * Calculate percentage of error rows
     */
    public double getErrorPercentage() {
        if (totalRows == 0) return 0.0;
        return (double) errorRows / totalRows * 100.0;
    }
    
    /**
     * Check if import can proceed (no error rows)
     */
    public boolean canProceed() {
        return errorRows == 0 && totalRows > 0;
    }
    
    /**
     * Get total number of errors across all fields
     */
    public int getTotalErrors() {
        return errorCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    /**
     * Get total number of warnings across all fields
     */
    public int getTotalWarnings() {
        return warningCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}

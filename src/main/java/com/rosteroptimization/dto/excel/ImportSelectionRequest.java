package com.rosteroptimization.dto.excel;

import lombok.Data;

import java.util.List;

/**
 * Request object for selective import of Excel rows
 */
@Data
public class ImportSelectionRequest {
    /**
     * List of row numbers to be imported (1-based)
     */
    private List<Integer> selectedRowNumbers;
    
    /**
     * Whether to skip rows with errors completely
     */
    private boolean skipErrors;
    
    /**
     * Whether to continue import even if some rows have warnings
     */
    private boolean continueOnWarnings;
    
    /**
     * Whether to apply suggested auto-corrections before import
     */
    private boolean applyAutoCorrections;
    
    /**
     * List of suggestion IDs to apply (if applyAutoCorrections is true)
     */
    private List<Integer> selectedSuggestions;
    
    /**
     * Whether to stop import on first error or continue with other rows
     */
    private boolean stopOnFirstError;
    
    public ImportSelectionRequest() {
        this.skipErrors = true;
        this.continueOnWarnings = true;
        this.applyAutoCorrections = false;
        this.stopOnFirstError = false;
    }
}

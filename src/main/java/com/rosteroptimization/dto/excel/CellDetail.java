package com.rosteroptimization.dto.excel;

import com.rosteroptimization.enums.CellStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed information about a single cell in Excel preview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CellDetail {
    /**
     * The actual value in the cell (original or parsed)
     */
    private Object value;
    
    /**
     * Status of the cell (VALID, WARNING, ERROR, INFO)
     */
    private CellStatus status;
    
    /**
     * Error or warning message for the cell
     */
    private String message;
    
    /**
     * Smart suggestion for fixing the cell value
     */
    private String suggestion;
    
    /**
     * Original value from Excel (if different from value)
     */
    private String originalValue;
    
    public CellDetail(Object value, CellStatus status) {
        this.value = value;
        this.status = status;
    }
    
    public CellDetail(Object value, CellStatus status, String message) {
        this.value = value;
        this.status = status;
        this.message = message;
    }
    
    public CellDetail(Object value, CellStatus status, String message, String suggestion) {
        this.value = value;
        this.status = status;
        this.message = message;
        this.suggestion = suggestion;
    }
}

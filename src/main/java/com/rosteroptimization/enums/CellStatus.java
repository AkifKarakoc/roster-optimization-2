package com.rosteroptimization.enums;

/**
 * Status for individual cells in Excel preview
 */
public enum CellStatus {
    /**
     * Cell data is valid - Green
     */
    VALID,
    
    /**
     * Cell has warnings but can be imported - Yellow
     */
    WARNING,
    
    /**
     * Cell has errors and cannot be imported - Red
     */
    ERROR,
    
    /**
     * Cell has informational message - Blue
     */
    INFO
}

package com.rosteroptimization.enums;

/**
 * Status for rows after import process
 */
public enum RowImportStatus {
    /**
     * Row has not been processed yet
     */
    NOT_PROCESSED,
    
    /**
     * Row was successfully imported to database
     */
    IMPORTED,
    
    /**
     * Row import failed
     */
    FAILED,
    
    /**
     * Row was skipped (not selected for import)
     */
    SKIPPED
}

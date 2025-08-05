package com.rosteroptimization.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a suggestion for auto-correction
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestionInfo {
    /**
     * Row number where the suggestion applies
     */
    private int rowNumber;
    
    /**
     * Column name where the suggestion applies
     */
    private String columnName;
    
    /**
     * Current (incorrect) value
     */
    private String currentValue;
    
    /**
     * Suggested (corrected) value
     */
    private String suggestedValue;
    
    /**
     * Reason for the suggestion
     */
    private String reason;
    
    /**
     * Confidence level of the suggestion (0.0 to 1.0)
     */
    private double confidence;
    
    /**
     * Type of suggestion (format_correction, foreign_key_match, etc.)
     */
    private String suggestionType;
}

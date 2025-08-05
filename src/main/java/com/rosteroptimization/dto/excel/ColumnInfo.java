package com.rosteroptimization.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Information about a column in Excel preview
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfo {
    /**
     * Column name (header)
     */
    private String name;
    
    /**
     * Data type expected for this column
     */
    private String type;
    
    /**
     * Whether this column is required
     */
    private boolean required;
    
    /**
     * Description or help text for this column
     */
    private String description;
    
    /**
     * Valid values for dropdown/selection fields
     */
    private List<String> validValues;
    
    /**
     * Sample/example value for this column
     */
    private String sampleValue;
    
    /**
     * Maximum length for string fields
     */
    private Integer maxLength;
    
    /**
     * Minimum value for numeric fields
     */
    private Number minValue;
    
    /**
     * Maximum value for numeric fields
     */
    private Number maxValue;
    
    /**
     * Pattern or format for validation (e.g., email, phone)
     */
    private String pattern;
}

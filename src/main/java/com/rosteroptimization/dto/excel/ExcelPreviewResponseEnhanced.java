package com.rosteroptimization.dto.excel;

import com.rosteroptimization.enums.ImportMode;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.Map;

/**
 * Enhanced Excel preview response with detailed cell-level information
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExcelPreviewResponseEnhanced extends ExcelPreviewResponse {
    
    /**
     * Information about each column in the Excel file
     * Key: column name, Value: column info
     */
    private Map<String, ColumnInfo> columnInfo;
    
    /**
     * Enhanced results with cell-level details
     */
    private List<ExcelRowPreview<?>> enhancedResults;
    
    /**
     * Detailed statistics about the preview
     */
    private PreviewStatistics statistics;
    
    /**
     * Suggested corrections that can be auto-applied
     */
    private List<SuggestionInfo> suggestions;
    
    /**
     * Whether auto-corrections are available
     */
    private boolean hasAutoCorrections;
    
    /**
     * Preview generation timestamp
     */
    private java.time.LocalDateTime previewGeneratedAt;
    
    public ExcelPreviewResponseEnhanced() {
        super();
        this.previewGeneratedAt = java.time.LocalDateTime.now();
        this.hasAutoCorrections = false;
    }
    
    /**
     * Override canProceed to use statistics
     */
    @Override
    public boolean isCanProceed() {
        return statistics != null && statistics.canProceed();
    }
    
    /**
     * Get count of selected rows
     */
    public int getSelectedRowCount() {
        if (enhancedResults == null) return 0;
        return (int) enhancedResults.stream()
                .filter(ExcelRowPreview::isSelected)
                .count();
    }
    
    /**
     * Check if any rows are selected for import
     */
    public boolean hasSelectedRows() {
        return getSelectedRowCount() > 0;
    }
    
    /**
     * Get enhanced results (alias for enhancedResults)
     */
    public List<ExcelRowPreview<?>> getRows() {
        return this.enhancedResults;
    }
    
    /**
     * Set enhanced results (alias for enhancedResults)
     */
    public void setRows(List<ExcelRowPreview<?>> rows) {
        this.enhancedResults = rows;
    }
}

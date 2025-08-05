package com.rosteroptimization.service.excel;

import com.rosteroptimization.dto.excel.*;
import org.springframework.web.multipart.MultipartFile;

public interface ExcelImportService {

    /**
     * Single entity import - parse and validate Excel file with enhanced preview
     */
    ExcelPreviewResponseEnhanced previewEntity(MultipartFile file, String entityType);

    /**
     * Multi entity import - parse and validate Excel file with multiple sheets
     */
    ExcelPreviewResponseEnhanced previewBulk(MultipartFile file);

    /**
     * Apply selected rows from preview session
     */
    ImportResult applySelectedChanges(String sessionId, ImportSelectionRequest request);

    // Legacy methods - kept for backward compatibility
    /**
     * @deprecated Use previewEntity instead
     */
    @Deprecated
    ExcelPreviewResponse parseEntity(MultipartFile file, String entityType);

    /**
     * @deprecated Use applySelectedChanges instead
     */
    @Deprecated
    ImportResult applyEntityChanges(String sessionId);

    /**
     * @deprecated Use previewBulk instead
     */
    @Deprecated
    ExcelPreviewResponse parseBulk(MultipartFile file);

    /**
     * @deprecated Use applySelectedChanges instead
     */
    @Deprecated
    ImportResult applyBulkChanges(String sessionId);

    /**
     * Generate Excel template for specific entity
     */
    byte[] generateTemplate(String entityType);

    /**
     * Generate Excel template for bulk import (multiple sheets)
     */
    byte[] generateBulkTemplate();

    /**
     * Get enhanced preview session details
     */
    ExcelPreviewResponseEnhanced getEnhancedSession(String sessionId);

    /**
     * @deprecated Use getEnhancedSession instead
     */
    @Deprecated
    ExcelPreviewResponse getSession(String sessionId);

    /**
     * Clear session data
     */
    void clearSession(String sessionId);

    /**
     * Update row selection in session
     */
    void updateRowSelection(String sessionId, int rowNumber, boolean selected);

    /**
     * Apply auto-corrections to session data
     */
    void applyAutoCorrections(String sessionId, ImportSelectionRequest request);

    /**
     * Get column information for entity type
     */
    java.util.Map<String, ColumnInfo> getColumnInfo(String entityType);
}
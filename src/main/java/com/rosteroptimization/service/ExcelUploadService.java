package com.rosteroptimization.service;

import com.rosteroptimization.service.excel.ExcelImportService;
import com.rosteroptimization.service.excel.ExcelParsingService;
import com.rosteroptimization.service.excel.ExcelProcessingException;
import com.rosteroptimization.service.excel.ExcelValidationService;
import com.rosteroptimization.service.excel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelUploadService {

    private final ExcelParsingService excelParsingService;
    private final ExcelValidationService excelValidationService;
    private final ExcelImportService excelImportService;

    // Cache for preview sessions (simple in-memory cache)
    private final Map<String, ExcelPreviewDTO> previewCache = new HashMap<>();

    /**
     * Parse and validate Excel file, return preview with errors
     * Phase 1: Parse & Validate
     */
    public ExcelPreviewDTO parseAndValidate(MultipartFile file) {
        log.info("Starting Excel parse and validation for file: {}", file.getOriginalFilename());

        try {
            // 1. Basic file validation
            validateFile(file);

            // 2. Parse Excel file to extract data
            List<ExcelSheetDTO> sheets = excelParsingService.parseExcelFile(file);
            log.info("Parsed {} sheets from Excel file", sheets.size());

            // 3. Validate all data and generate errors
            ExcelPreviewDTO preview = excelValidationService.validateAndCreatePreview(sheets);

            // 4. Cache preview for later import
            String sessionId = UUID.randomUUID().toString();
            preview.setSessionId(sessionId);
            previewCache.put(sessionId, preview);

            log.info("Excel validation completed. Session ID: {}, Total rows: {}, Error rows: {}",
                    sessionId, preview.getTotalRows(), preview.getErrorRows());

            return preview;

        } catch (Exception e) {
            log.error("Error during Excel parsing and validation: {}", e.getMessage(), e);
            throw new ExcelProcessingException("Failed to parse Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Import selected rows from cached preview
     * Phase 2: Import Selected
     */
    public ImportResultDTO importSelectedRows(String sessionId, ImportRequestDTO importRequest) {
        log.info("Starting import for session: {}, Selected rows: {}", sessionId, importRequest.getSelectedRowIds().size());

        try {
            // 1. Get cached preview
            ExcelPreviewDTO preview = previewCache.get(sessionId);
            if (preview == null) {
                throw new ExcelProcessingException("Preview session not found or expired: " + sessionId);
            }

            // 2. Filter selected rows
            List<ExcelRowDTO> selectedRows = filterSelectedRows(preview, importRequest.getSelectedRowIds());

            // 3. Import in dependency order
            ImportResultDTO result = excelImportService.importRows(selectedRows);

            // 4. Cleanup cache
            previewCache.remove(sessionId);

            log.info("Import completed for session: {}. Success: {}, Failed: {}",
                    sessionId, result.getSuccessCount(), result.getFailureCount());

            return result;

        } catch (Exception e) {
            log.error("Error during Excel import: {}", e.getMessage(), e);
            throw new ExcelProcessingException("Failed to import Excel data: " + e.getMessage(), e);
        }
    }

    /**
     * Get preview by session ID (for frontend to re-fetch)
     */
    public ExcelPreviewDTO getPreview(String sessionId) {
        ExcelPreviewDTO preview = previewCache.get(sessionId);
        if (preview == null) {
            throw new ExcelProcessingException("Preview session not found or expired: " + sessionId);
        }
        return preview;
    }

    /**
     * Clear preview cache (cleanup method)
     */
    public void clearPreview(String sessionId) {
        previewCache.remove(sessionId);
        log.info("Preview cache cleared for session: {}", sessionId);
    }

    /**
     * Get all cached preview sessions (for admin/debug)
     */
    public List<String> getActiveSessions() {
        return new ArrayList<>(previewCache.keySet());
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ExcelProcessingException("File is empty or null");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new ExcelProcessingException("File must be Excel format (.xlsx or .xls)");
        }

        // 10MB limit
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            throw new ExcelProcessingException("File size cannot exceed 10MB");
        }

        log.debug("File validation passed: {} ({} bytes)", filename, file.getSize());
    }

    /**
     * Filter selected rows from preview
     */
    private List<ExcelRowDTO> filterSelectedRows(ExcelPreviewDTO preview, List<String> selectedRowIds) {
        List<ExcelRowDTO> selectedRows = new ArrayList<>();

        for (ExcelSheetDTO sheet : preview.getSheets()) {
            for (ExcelRowDTO row : sheet.getRows()) {
                if (selectedRowIds.contains(row.getRowId())) {
                    // Only import rows that can be imported (no ERROR level issues)
                    if (row.isCanImport()) {
                        selectedRows.add(row);
                    } else {
                        log.warn("Skipping row {} because it has blocking errors", row.getRowId());
                    }
                }
            }
        }

        log.info("Filtered {} importable rows from {} selected", selectedRows.size(), selectedRowIds.size());
        return selectedRows;
    }
}
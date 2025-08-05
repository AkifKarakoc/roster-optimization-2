package com.rosteroptimization.controller;

import com.rosteroptimization.dto.excel.*;
import com.rosteroptimization.service.excel.ExcelImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
public class ExcelImportController {

    private final ExcelImportService excelImportService;

    // New enhanced endpoints
    @PostMapping("/preview/{entityType}")
    public ResponseEntity<ExcelPreviewResponseEnhanced> previewEntity(
            @PathVariable String entityType,
            @RequestParam("file") MultipartFile file) {

        log.info("Received entity preview request for: {}", entityType);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ExcelPreviewResponseEnhanced response = excelImportService.previewEntity(file, entityType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview/bulk")
    public ResponseEntity<ExcelPreviewResponseEnhanced> previewBulk(
            @RequestParam("file") MultipartFile file) {

        log.info("Received bulk preview request");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ExcelPreviewResponseEnhanced response = excelImportService.previewBulk(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/{sessionId}")
    public ResponseEntity<ImportResult> importSelected(
            @PathVariable String sessionId,
            @RequestBody ImportSelectionRequest request) {

        log.info("Importing selected rows for session: {}, selected rows: {}", 
                sessionId, request.getSelectedRowNumbers().size());

        ExcelPreviewResponseEnhanced session = excelImportService.getEnhancedSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        ImportResult result = excelImportService.applySelectedChanges(sessionId, request);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/session/{sessionId}/selection")
    public ResponseEntity<Void> updateRowSelection(
            @PathVariable String sessionId,
            @RequestParam int rowNumber,
            @RequestParam boolean selected) {
        
        excelImportService.updateRowSelection(sessionId, rowNumber, selected);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/session/{sessionId}/auto-correct")
    public ResponseEntity<Void> applyAutoCorrections(
            @PathVariable String sessionId,
            @RequestBody ImportSelectionRequest request) {
        
        excelImportService.applyAutoCorrections(sessionId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/session/{sessionId}/enhanced")
    public ResponseEntity<ExcelPreviewResponseEnhanced> getEnhancedSession(@PathVariable String sessionId) {
        ExcelPreviewResponseEnhanced session = excelImportService.getEnhancedSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/column-info/{entityType}")
    public ResponseEntity<java.util.Map<String, ColumnInfo>> getColumnInfo(@PathVariable String entityType) {
        java.util.Map<String, ColumnInfo> columnInfo = excelImportService.getColumnInfo(entityType);
        return ResponseEntity.ok(columnInfo);
    }

    // Legacy endpoints - kept for backward compatibility
    @PostMapping("/import/{entityType}")
    @Deprecated
    public ResponseEntity<ExcelPreviewResponse> uploadSingleEntityLegacy(
            @PathVariable String entityType,
            @RequestParam("file") MultipartFile file) {

        log.info("Received single entity upload request for: {}", entityType);

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ExcelPreviewResponse response = excelImportService.parseEntity(file, entityType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/import/bulk")
    @Deprecated
    public ResponseEntity<ExcelPreviewResponse> uploadBulkLegacy(
            @RequestParam("file") MultipartFile file) {

        log.info("Received bulk upload request");

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ExcelPreviewResponse response = excelImportService.parseBulk(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply/{sessionId}")
    @Deprecated
    public ResponseEntity<ImportResult> applyChangesLegacy(@PathVariable String sessionId) {
        log.info("Applying changes for session: {}", sessionId);

        ExcelPreviewResponse session = excelImportService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }

        ImportResult result;
        if (session.getMode() == com.rosteroptimization.enums.ImportMode.SINGLE) {
            result = excelImportService.applyEntityChanges(sessionId);
        } else {
            result = excelImportService.applyBulkChanges(sessionId);
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/session/{sessionId}")
    @Deprecated
    public ResponseEntity<ExcelPreviewResponse> getSessionLegacy(@PathVariable String sessionId) {
        ExcelPreviewResponse session = excelImportService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> clearSession(@PathVariable String sessionId) {
        excelImportService.clearSession(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/template/{entityType}")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable String entityType) {
        log.info("Template download request for: {}", entityType);

        byte[] template = excelImportService.generateTemplate(entityType);
        
        if (template.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entityType + "_template.xlsx\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        log.info("Template generated successfully. Size: {} bytes", template.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(template);
    }

    @GetMapping("/template/bulk")
    public ResponseEntity<byte[]> downloadBulkTemplate() {
        log.info("Bulk template download request");

        byte[] template = excelImportService.generateBulkTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=bulk_import_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(template);
    }
}

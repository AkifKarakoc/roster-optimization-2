package com.rosteroptimization.controller;

import com.rosteroptimization.service.ExcelUploadService;
import com.rosteroptimization.service.excel.ExcelProcessingException;
import com.rosteroptimization.service.excel.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST Controller for Excel upload and import operations
 */
@RestController
@RequestMapping("/api/excel")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Excel Upload", description = "Excel file upload, validation and import operations")
public class ExcelUploadController {

    private final ExcelUploadService excelUploadService;

    /**
     * Upload and validate Excel file, return preview with validation results
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and validate Excel file",
            description = "Parse Excel file, validate data and return preview with errors for user review",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File processed successfully",
                            content = @Content(schema = @Schema(implementation = ExcelPreviewDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid file or validation errors"),
                    @ApiResponse(responseCode = "413", description = "File too large"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ExcelPreviewDTO> uploadAndValidate(
            @Parameter(description = "Excel file (.xlsx or .xls)", required = true)
            @RequestParam("file") MultipartFile file) {

        log.info("Received Excel upload request: {} ({} bytes)",
                file.getOriginalFilename(), file.getSize());

        try {
            ExcelPreviewDTO preview = excelUploadService.parseAndValidate(file);
            preview.setFileName(file.getOriginalFilename()); // Set original filename

            log.info("Excel upload processed successfully. Session: {}, Rows: {}, Errors: {}",
                    preview.getSessionId(), preview.getTotalRows(), preview.getErrorRows());

            return ResponseEntity.ok(preview);

        } catch (ExcelProcessingException e) {
            log.warn("Excel processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during Excel upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Import selected rows from validated Excel preview
     */
    @PostMapping(value = "/import/{sessionId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Import selected Excel rows",
            description = "Import selected rows from previously uploaded and validated Excel file",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Import completed",
                            content = @Content(schema = @Schema(implementation = ImportResultDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid session or request"),
                    @ApiResponse(responseCode = "404", description = "Session not found or expired"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<ImportResultDTO> importSelectedRows(
            @Parameter(description = "Session ID from upload response", required = true)
            @PathVariable String sessionId,
            @Parameter(description = "Import request with selected row IDs", required = true)
            @Valid @RequestBody ImportRequestDTO importRequest) {

        log.info("Received import request for session: {} with {} selected rows",
                sessionId, importRequest.getSelectedCount());

        try {
            // Validate import request
            importRequest.validate();

            ImportResultDTO result = excelUploadService.importSelectedRows(sessionId, importRequest);

            log.info("Import completed for session: {}. Success: {}, Failed: {}",
                    sessionId, result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(result);

        } catch (ExcelProcessingException e) {
            log.warn("Import failed for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid import request for session {}: {}", sessionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Unexpected error during import for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get preview by session ID (for re-fetching or debugging)
     */
    @GetMapping("/preview/{sessionId}")
    @Operation(
            summary = "Get Excel preview by session ID",
            description = "Retrieve previously generated Excel preview for review or debugging",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Preview retrieved successfully",
                            content = @Content(schema = @Schema(implementation = ExcelPreviewDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Session not found or expired")
            }
    )
    public ResponseEntity<ExcelPreviewDTO> getPreview(
            @Parameter(description = "Session ID from upload response", required = true)
            @PathVariable String sessionId) {

        log.debug("Retrieving preview for session: {}", sessionId);

        try {
            ExcelPreviewDTO preview = excelUploadService.getPreview(sessionId);
            return ResponseEntity.ok(preview);
        } catch (ExcelProcessingException e) {
            log.warn("Preview not found for session: {}", sessionId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Clear preview cache for session (cleanup)
     */
    @DeleteMapping("/preview/{sessionId}")
    @Operation(
            summary = "Clear Excel preview session",
            description = "Clear cached preview data for the specified session (cleanup)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Session cleared successfully"),
                    @ApiResponse(responseCode = "404", description = "Session not found")
            }
    )
    public ResponseEntity<Void> clearPreview(
            @Parameter(description = "Session ID to clear", required = true)
            @PathVariable String sessionId) {

        log.debug("Clearing preview for session: {}", sessionId);

        try {
            excelUploadService.clearPreview(sessionId);
            return ResponseEntity.ok().build();
        } catch (ExcelProcessingException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get active sessions (for admin/debugging)
     */
    @GetMapping("/sessions")
    @Operation(
            summary = "Get active Excel sessions",
            description = "List all active Excel upload sessions (admin/debug endpoint)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Active sessions retrieved")
            }
    )
    public ResponseEntity<List<String>> getActiveSessions() {
        log.debug("Retrieving active Excel sessions");

        List<String> sessions = excelUploadService.getActiveSessions();
        return ResponseEntity.ok(sessions);
    }

    /**
     * Health check endpoint for Excel upload functionality
     */
    @GetMapping("/health")
    @Operation(
            summary = "Excel upload service health check",
            description = "Check if Excel upload service is working properly",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Service is healthy"),
                    @ApiResponse(responseCode = "503", description = "Service is unhealthy")
            }
    )
    public ResponseEntity<String> healthCheck() {
        try {
            // Simple health check - could be enhanced with more detailed checks
            int activeSessions = excelUploadService.getActiveSessions().size();
            String status = String.format("Excel upload service is healthy. Active sessions: %d", activeSessions);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Excel upload service health check failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("Excel upload service is unhealthy: " + e.getMessage());
        }
    }

    /**
     * Global exception handler for Excel-specific exceptions
     */
    @ExceptionHandler(ExcelProcessingException.class)
    public ResponseEntity<ErrorResponseDTO> handleExcelProcessingException(ExcelProcessingException e) {
        log.warn("Excel processing error: {}", e.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .error("EXCEL_PROCESSING_ERROR")
                .message(e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Global exception handler for validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationException(IllegalArgumentException e) {
        log.warn("Validation error: {}", e.getMessage());

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .error("VALIDATION_ERROR")
                .message(e.getMessage())
                .timestamp(java.time.LocalDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    /**
     * Error response DTO for API responses
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponseDTO {
        private String error;
        private String message;
        private java.time.LocalDateTime timestamp;
    }
}
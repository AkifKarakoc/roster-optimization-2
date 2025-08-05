package com.rosteroptimization.service.excel.impl;

import com.rosteroptimization.dto.excel.*;
import com.rosteroptimization.enums.ImportMode;
import com.rosteroptimization.service.excel.ExcelBulkOperationService;
import com.rosteroptimization.service.excel.ExcelImportService;
import com.rosteroptimization.service.excel.ExcelValidationService;
import com.rosteroptimization.service.excel.parser.EntityExcelParser;
import com.rosteroptimization.service.excel.parser.EntityParserFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelImportServiceImpl implements ExcelImportService {

    private final EntityParserFactory parserFactory;
    private final ExcelBulkOperationService bulkOperationService;
    private final ExcelValidationService validationService;
    private final Map<String, ExcelPreviewResponse> sessionStorage = new ConcurrentHashMap<>();
    private final Map<String, ExcelPreviewResponseEnhanced> enhancedSessionStorage = new ConcurrentHashMap<>();

    // New enhanced endpoints
    @Override
    public ExcelPreviewResponseEnhanced previewEntity(MultipartFile file, String entityType) {
        log.info("Starting enhanced entity preview for type: {}", entityType);

        String sessionId = UUID.randomUUID().toString();
        ExcelPreviewResponseEnhanced response = validationService.validateEntity(file, entityType, sessionId);
        
        if (response != null) {
            enhancedSessionStorage.put(sessionId, response);
        }
        
        return response;
    }

    @Override
    public ExcelPreviewResponseEnhanced previewBulk(MultipartFile file) {
        log.info("Starting enhanced bulk preview");

        String sessionId = UUID.randomUUID().toString();
        ExcelPreviewResponseEnhanced response = validationService.validateBulk(file, sessionId);
        
        if (response != null) {
            enhancedSessionStorage.put(sessionId, response);
        }
        
        return response;
    }

    @Override
    public ImportResult applySelectedChanges(String sessionId, ImportSelectionRequest request) {
        log.info("Applying selected changes for session: {}", sessionId);

        ExcelPreviewResponseEnhanced session = enhancedSessionStorage.get(sessionId);
        if (session == null) {
            log.error("Session not found: {}", sessionId);
            ImportResult result = new ImportResult();
            result.setSuccess(false);
            result.setMessage("Session not found");
            return result;
        }

        // Filter selected rows
        List<ExcelRowPreview<?>> selectedRows = session.getRows().stream()
                .filter(row -> request.getSelectedRowNumbers().contains(row.getRowNumber()))
                .toList();

        if (selectedRows.isEmpty()) {
            ImportResult result = new ImportResult();
            result.setSuccess(false);
            result.setMessage("No rows selected for import");
            return result;
        }

        // Apply changes via bulk operation service
        return bulkOperationService.applySelectedRows(selectedRows, session.getEntityType());
    }

    @Override
    public ExcelPreviewResponseEnhanced getEnhancedSession(String sessionId) {
        return enhancedSessionStorage.get(sessionId);
    }

    @Override
    public void updateRowSelection(String sessionId, int rowNumber, boolean selected) {
        ExcelPreviewResponseEnhanced session = enhancedSessionStorage.get(sessionId);
        if (session != null) {
            session.getRows().stream()
                    .filter(row -> row.getRowNumber() == rowNumber)
                    .findFirst()
                    .ifPresent(row -> row.setSelected(selected));
        }
    }

    @Override
    public void applyAutoCorrections(String sessionId, ImportSelectionRequest request) {
        ExcelPreviewResponseEnhanced session = enhancedSessionStorage.get(sessionId);
        if (session != null) {
            validationService.applyAutoCorrections(session, request);
        }
    }

    @Override
    public java.util.Map<String, ColumnInfo> getColumnInfo(String entityType) {
        return validationService.getColumnInfo(entityType);
    }

    @Override
    public ExcelPreviewResponse parseEntity(MultipartFile file, String entityType) {
        log.info("Starting single entity import for type: {}", entityType);

        String sessionId = UUID.randomUUID().toString();
        ExcelPreviewResponse response = new ExcelPreviewResponse();
        response.setSessionId(sessionId);
        response.setMode(ImportMode.SINGLE);
        response.setEntityType(entityType);

        try {
            // Get parser for entity type
            EntityExcelParser<?> parser = parserFactory.getParser(entityType);
            if (parser == null) {
                response.setCanProceed(false);
                log.error("No parser found for entity type: {}", entityType);
                return response;
            }

            Workbook workbook = new XSSFWorkbook(file.getInputStream());

            if (workbook.getNumberOfSheets() == 0) {
                response.setCanProceed(false);
                log.error("Excel file has no sheets");
                workbook.close();
                return response;
            }

            Sheet sheet = workbook.getSheetAt(0);

            // Validate headers
            if (!validateHeaders(sheet, parser.getExpectedHeaders())) {
                response.setCanProceed(false);
                log.error("Invalid headers in Excel file");
                workbook.close();
                return response;
            }

            // Parse rows - Type safety fix
            @SuppressWarnings("unchecked")
            List<ExcelRowResult<?>> results = (List<ExcelRowResult<?>>) (List<?>) parser.parseRows(sheet);

            // Calculate statistics
            int totalRows = results.size();
            int validRows = 0;
            int invalidRows = 0;
            Map<String, Integer> operationCounts = new HashMap<>();

            for (ExcelRowResult<?> result : results) {
                if (result.isValid()) {
                    validRows++;
                } else {
                    invalidRows++;
                }

                if (result.getOperation() != null) {
                    String operation = result.getOperation().name();
                    operationCounts.put(operation, operationCounts.getOrDefault(operation, 0) + 1);
                }
            }

            response.setTotalRows(totalRows);
            response.setValidRows(validRows);
            response.setInvalidRows(invalidRows);
            response.setResults(results);
            response.setOperationCounts(operationCounts);
            response.setCanProceed(invalidRows == 0 && totalRows > 0);

            workbook.close();

            // Store session
            sessionStorage.put(sessionId, response);

            log.info("Completed parsing for session: {}. Total: {}, Valid: {}, Invalid: {}",
                    sessionId, totalRows, validRows, invalidRows);

        } catch (IOException e) {
            log.error("Error parsing Excel file: ", e);
            response.setCanProceed(false);
        }

        return response;
    }

    @Override
    public ImportResult applyEntityChanges(String sessionId) {
        log.info("Applying changes for session: {}", sessionId);

        ImportResult result = new ImportResult();
        result.setSessionId(sessionId);

        ExcelPreviewResponse session = sessionStorage.get(sessionId);
        if (session == null) {
            result.setSuccess(false);
            result.setMessage("Session not found or expired");
            return result;
        }

        try {
            String entityType = session.getEntityType().toLowerCase();
            List<ExcelRowResult<?>> results = session.getResults();

            // Use the generic method from bulk operation service
            result = bulkOperationService.applyOperationsByEntityType(entityType, results);
            result.setSessionId(sessionId);

        } catch (Exception e) {
            log.error("Error applying changes for session {}: ", sessionId, e);
            result.setSuccess(false);
            result.setMessage("Error applying changes: " + e.getMessage());
        }

        // Clear session after processing (successful or not)
        sessionStorage.remove(sessionId);

        return result;
    }

    @Override
    public ExcelPreviewResponse parseBulk(MultipartFile file) {
        log.info("Starting bulk import");

        String sessionId = UUID.randomUUID().toString();
        ExcelPreviewResponse response = new ExcelPreviewResponse();
        response.setSessionId(sessionId);
        response.setMode(ImportMode.BULK);

        try {
            Workbook workbook = new XSSFWorkbook(file.getInputStream());

            int numberOfSheets = workbook.getNumberOfSheets();
            log.info("Found {} sheets in workbook", numberOfSheets);

            List<ExcelRowResult<?>> allResults = new ArrayList<>();
            Map<String, Integer> operationCounts = new HashMap<>();
            int totalRows = 0;
            int validRows = 0;
            int invalidRows = 0;

            // Process each sheet
            for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String sheetName = sheet.getSheetName();

                log.info("Processing sheet: {}", sheetName);

                // Get parser for this sheet's entity type
                EntityExcelParser<?> parser = parserFactory.getParser(sheetName);
                if (parser == null) {
                    log.warn("No parser found for sheet: {}", sheetName);
                    continue;
                }

                // Validate headers
                if (!validateHeaders(sheet, parser.getExpectedHeaders())) {
                    log.error("Invalid headers in sheet: {}", sheetName);
                    continue;
                }

                // Parse rows
                @SuppressWarnings("unchecked")
                List<ExcelRowResult<?>> sheetResults = (List<ExcelRowResult<?>>) (List<?>) parser.parseRows(sheet);

                // Update statistics
                for (ExcelRowResult<?> result : sheetResults) {
                    totalRows++;
                    if (result.isValid()) {
                        validRows++;
                    } else {
                        invalidRows++;
                    }

                    if (result.getOperation() != null) {
                        String operation = result.getOperation().name();
                        operationCounts.put(operation, operationCounts.getOrDefault(operation, 0) + 1);
                    }
                }

                allResults.addAll(sheetResults);
            }

            response.setTotalRows(totalRows);
            response.setValidRows(validRows);
            response.setInvalidRows(invalidRows);
            response.setResults(allResults);
            response.setOperationCounts(operationCounts);
            response.setCanProceed(invalidRows == 0 && totalRows > 0);

            workbook.close();
            sessionStorage.put(sessionId, response);

            log.info("Completed bulk parsing for session: {}. Total: {}, Valid: {}, Invalid: {}",
                    sessionId, totalRows, validRows, invalidRows);

        } catch (IOException e) {
            log.error("Error parsing bulk Excel file: ", e);
            response.setCanProceed(false);
        }

        return response;
    }

    @Override
    public ImportResult applyBulkChanges(String sessionId) {
        log.info("Applying bulk changes for session: {}", sessionId);

        ImportResult result = new ImportResult();
        result.setSessionId(sessionId);

        ExcelPreviewResponse session = sessionStorage.get(sessionId);
        if (session == null) {
            result.setSuccess(false);
            result.setMessage("Session not found or expired");
            return result;
        }

        try {
            List<ExcelRowResult<?>> allResults = session.getResults();

            // Group results by entity type (based on parser type or sheet name)
            Map<String, List<ExcelRowResult<?>>> resultsByEntity = groupResultsByEntityType(allResults);

            // Apply operations in dependency order
            String[] entityOrder = {
                    "department", "qualification", "workingperiod", "squadworkingpattern",
                    "squad", "shift", "staff", "task", "dayoffrule", "constraintoverride"
            };

            int totalProcessed = 0;
            int totalSuccessful = 0;
            int totalFailed = 0;
            Map<String, Integer> allAppliedCounts = new HashMap<>();

            for (String entityType : entityOrder) {
                List<ExcelRowResult<?>> entityResults = resultsByEntity.get(entityType);
                if (entityResults != null && !entityResults.isEmpty()) {
                    ImportResult entityResult = bulkOperationService.applyOperationsByEntityType(entityType, entityResults);

                    totalProcessed += entityResult.getTotalProcessed();
                    totalSuccessful += entityResult.getSuccessfulOperations();
                    totalFailed += entityResult.getFailedOperations();

                    // Merge operation counts
                    if (entityResult.getAppliedCounts() != null) {
                        entityResult.getAppliedCounts().forEach((key, value) ->
                                allAppliedCounts.put(key, allAppliedCounts.getOrDefault(key, 0) + value));
                    }
                }
            }

            result.setSuccess(totalFailed == 0);
            result.setTotalProcessed(totalProcessed);
            result.setSuccessfulOperations(totalSuccessful);
            result.setFailedOperations(totalFailed);
            result.setAppliedCounts(allAppliedCounts);
            result.setMessage(String.format("Processed %d operations across multiple entities. Success: %d, Failed: %d",
                    totalProcessed, totalSuccessful, totalFailed));

        } catch (Exception e) {
            log.error("Error applying bulk changes for session {}: ", sessionId, e);
            result.setSuccess(false);
            result.setMessage("Error applying bulk changes: " + e.getMessage());
        }

        // Clear session after processing
        sessionStorage.remove(sessionId);

        return result;
    }

    @Override
    public byte[] generateTemplate(String entityType) {
        log.info("Generating template for entity: {}", entityType);

        EntityExcelParser<?> parser = parserFactory.getParser(entityType);
        if (parser == null) {
            log.error("No parser found for entity type: {}", entityType);
            return new byte[0];
        }

        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet(entityType);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = parser.getExpectedHeaders();

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                // Style headers
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                cell.setCellStyle(headerStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Add sample data rows for reference
            addSampleDataRows(sheet, entityType, headers);

            // Convert to bytes
            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            byte[] result = outputStream.toByteArray();
            workbook.close();
            outputStream.close();

            log.info("Template generated successfully. Size: {} bytes", result.length);
            return result;

        } catch (IOException e) {
            log.error("Error generating template: ", e);
            return new byte[0];
        }
    }

    @Override
    public byte[] generateBulkTemplate() {
        log.info("Generating bulk template");

        try {
            Workbook workbook = new XSSFWorkbook();

            // Create sheets for all supported entities in dependency order
            String[] entities = {
                    "Department", "Qualification", "WorkingPeriod", // No dependencies
                    "SquadWorkingPattern", // No dependencies
                    "Squad", "Shift", // Depend on above
                    "Staff", "Task", // Depend on above
                    "DayOffRule", "ConstraintOverride" // Depend on Staff
            };

            for (String entity : entities) {
                EntityExcelParser<?> parser = parserFactory.getParser(entity);
                if (parser != null) {
                    Sheet sheet = workbook.createSheet(entity);

                    // Create header row
                    Row headerRow = sheet.createRow(0);
                    String[] headers = parser.getExpectedHeaders();

                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = headerRow.createCell(i);
                        cell.setCellValue(headers[i]);

                        // Style headers
                        CellStyle headerStyle = workbook.createCellStyle();
                        Font headerFont = workbook.createFont();
                        headerFont.setBold(true);
                        headerStyle.setFont(headerFont);
                        cell.setCellStyle(headerStyle);
                    }

                    // Auto-size columns
                    for (int i = 0; i < headers.length; i++) {
                        sheet.autoSizeColumn(i);
                    }

                    // Add sample data
                    addSampleDataRows(sheet, entity, headers);
                }
            }

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("Error generating bulk template: ", e);
            return new byte[0];
        }
    }

    @Override
    public ExcelPreviewResponse getSession(String sessionId) {
        return sessionStorage.get(sessionId);
    }

    @Override
    public void clearSession(String sessionId) {
        sessionStorage.remove(sessionId);
        enhancedSessionStorage.remove(sessionId);
        log.info("Cleared session: {}", sessionId);
    }

    private boolean validateHeaders(Sheet sheet, String[] expectedHeaders) {
        if (sheet.getLastRowNum() < 0) {
            return false;
        }

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            return false;
        }

        // Check if we have at least the minimum required headers
        int expectedHeaderCount = expectedHeaders.length;
        int actualHeaderCount = headerRow.getLastCellNum();

        if (actualHeaderCount < expectedHeaderCount) {
            log.warn("Expected {} headers, found {}", expectedHeaderCount, actualHeaderCount);
            return false;
        }

        // Validate header names (case-insensitive)
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) {
                log.warn("Missing header at position {}", i);
                return false;
            }

            String actualHeader = cell.getStringCellValue().trim().toUpperCase();
            String expectedHeader = expectedHeaders[i].toUpperCase();

            if (!actualHeader.equals(expectedHeader)) {
                log.warn("Invalid header at position {}. Expected: {}, Found: {}",
                        i, expectedHeader, actualHeader);
                return false;
            }
        }

        return true;
    }

    private void addSampleDataRows(Sheet sheet, String entityType, String[] headers) {
        switch (entityType.toLowerCase()) {
            case "department":
                addDepartmentSampleData(sheet, headers);
                break;
            case "qualification":
                addQualificationSampleData(sheet, headers);
                break;
            case "workingperiod":
                addWorkingPeriodSampleData(sheet, headers);
                break;
            case "squadworkingpattern":
                addSquadWorkingPatternSampleData(sheet, headers);
                break;
            case "shift":
                addShiftSampleData(sheet, headers);
                break;
            case "squad":
                addSquadSampleData(sheet, headers);
                break;
            case "staff":
                addStaffSampleData(sheet, headers);
                break;
            case "task":
                addTaskSampleData(sheet, headers);
                break;
            case "dayoffrule":
                addDayOffRuleSampleData(sheet, headers);
                break;
            case "constraintoverride":
                addConstraintOverrideSampleData(sheet, headers);
                break;
        }
    }

    private void addDepartmentSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("IT Department");
        sampleRow.createCell(3).setCellValue("Information Technology Department");
        sampleRow.createCell(4).setCellValue("true");
    }

    private void addQualificationSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("Java Programming");
        sampleRow.createCell(3).setCellValue("Java Development Skills");
        sampleRow.createCell(4).setCellValue("true");
    }

    private void addWorkingPeriodSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("Day Shift");
        sampleRow.createCell(3).setCellValue("08:00:00");
        sampleRow.createCell(4).setCellValue("17:00:00");
        sampleRow.createCell(5).setCellValue("Standard day working period");
        sampleRow.createCell(6).setCellValue("true");
    }

    private void addSquadWorkingPatternSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("4-Day Pattern");
        sampleRow.createCell(3).setCellValue("Shift_1,Shift_2,DayOff,DayOff");
        sampleRow.createCell(4).setCellValue("4");
        sampleRow.createCell(5).setCellValue("4-day rotating pattern");
        sampleRow.createCell(6).setCellValue("true");
    }

    private void addShiftSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("Morning Shift");
        sampleRow.createCell(3).setCellValue("08:00:00");
        sampleRow.createCell(4).setCellValue("16:00:00");
        sampleRow.createCell(5).setCellValue("false");
        sampleRow.createCell(6).setCellValue("true");
        sampleRow.createCell(7).setCellValue("Day Shift");
        sampleRow.createCell(8).setCellValue("Standard morning shift");
        sampleRow.createCell(9).setCellValue("true");
    }

    private void addSquadSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("Squad Alpha");
        sampleRow.createCell(3).setCellValue("2025-08-01");
        sampleRow.createCell(4).setCellValue("4-Day Pattern");
        sampleRow.createCell(5).setCellValue("Alpha team squad");
        sampleRow.createCell(6).setCellValue("true");
    }

    private void addStaffSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("John");
        sampleRow.createCell(3).setCellValue("Doe");
        sampleRow.createCell(4).setCellValue("EMP001");
        sampleRow.createCell(5).setCellValue("john.doe@company.com");
        sampleRow.createCell(6).setCellValue("+1234567890");
        sampleRow.createCell(7).setCellValue("IT Department");
        sampleRow.createCell(8).setCellValue("Squad Alpha");
        sampleRow.createCell(9).setCellValue("Java Programming,SQL");
        sampleRow.createCell(10).setCellValue("true");
    }

    private void addTaskSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("System Maintenance");
        sampleRow.createCell(3).setCellValue("2025-08-10T08:00:00");
        sampleRow.createCell(4).setCellValue("2025-08-10T12:00:00");
        sampleRow.createCell(5).setCellValue("2");
        sampleRow.createCell(6).setCellValue("IT Department");
        sampleRow.createCell(7).setCellValue("Java Programming,SQL");
        sampleRow.createCell(8).setCellValue("Regular system maintenance task");
        sampleRow.createCell(9).setCellValue("true");
    }

    private void addDayOffRuleSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("5");
        sampleRow.createCell(3).setCellValue("2");
        sampleRow.createCell(4).setCellValue("SATURDAY,SUNDAY");
        sampleRow.createCell(5).setCellValue("EMP001");
    }

    private void addConstraintOverrideSampleData(Sheet sheet, String[] headers) {
        Row sampleRow = sheet.createRow(1);
        sampleRow.createCell(0).setCellValue("ADD");
        sampleRow.createCell(1).setCellValue(""); // ID - empty for ADD
        sampleRow.createCell(2).setCellValue("10");
        sampleRow.createCell(3).setCellValue("EMP001");
        sampleRow.createCell(4).setCellValue("Max Working Hours/Day");
    }

    /**
     * Group results by entity type - this is a simplified approach
     * In a more sophisticated implementation, we might track entity type per result
     */
    private Map<String, List<ExcelRowResult<?>>> groupResultsByEntityType(List<ExcelRowResult<?>> allResults) {
        Map<String, List<ExcelRowResult<?>>> grouped = new HashMap<>();

        // For now, we'll need to determine entity type from the DTO class
        for (ExcelRowResult<?> result : allResults) {
            if (result.getData() != null) {
                String entityType = determineEntityType(result.getData());
                if (entityType != null) {
                    grouped.computeIfAbsent(entityType, k -> new ArrayList<>()).add(result);
                }
            }
        }

        return grouped;
    }

    private String determineEntityType(Object dto) {
        String className = dto.getClass().getSimpleName();
        if (className.endsWith("DTO")) {
            String entityName = className.substring(0, className.length() - 3);
            return entityName.toLowerCase();
        }
        return null;
    }
}
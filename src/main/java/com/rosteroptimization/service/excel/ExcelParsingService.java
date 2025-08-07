package com.rosteroptimization.service.excel;

import com.rosteroptimization.service.excel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelParsingService {

    /**
     * Parse Excel file and extract all sheets with data
     */
    public List<ExcelSheetDTO> parseExcelFile(MultipartFile file) {
        log.info("Parsing Excel file: {}", file.getOriginalFilename());

        List<ExcelSheetDTO> sheets = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = createWorkbook(file.getOriginalFilename(), inputStream);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                log.debug("Processing sheet: {}", sheetName);

                // Skip empty sheets
                if (sheet.getPhysicalNumberOfRows() == 0) {
                    log.warn("Skipping empty sheet: {}", sheetName);
                    continue;
                }

                ExcelSheetDTO sheetDTO = parseSheet(sheet, sheetName);
                if (sheetDTO != null && !sheetDTO.getRows().isEmpty()) {
                    sheets.add(sheetDTO);
                    log.info("Parsed sheet '{}' with {} rows", sheetName, sheetDTO.getRows().size());
                }
            }

            workbook.close();

        } catch (Exception e) {
            log.error("Error parsing Excel file: {}", e.getMessage(), e);
            throw new ExcelProcessingException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        if (sheets.isEmpty()) {
            throw new ExcelProcessingException("No valid sheets found in Excel file");
        }

        return sheets;
    }

    /**
     * Create appropriate workbook based on file extension
     */
    private Workbook createWorkbook(String filename, InputStream inputStream) throws Exception {
        if (filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream);
        } else if (filename.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream);
        } else {
            throw new ExcelProcessingException("Unsupported file format. Only .xlsx and .xls are supported.");
        }
    }

    /**
     * Parse single sheet and convert to DTO
     */
    private ExcelSheetDTO parseSheet(Sheet sheet, String sheetName) {
        try {
            // Detect entity type from sheet name
            ExcelEntityType entityType = detectEntityType(sheetName);
            if (entityType == null) {
                log.warn("Unknown entity type for sheet: {}, skipping", sheetName);
                return null;
            }

            // Parse header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.warn("No header row found in sheet: {}", sheetName);
                return null;
            }

            List<String> headers = parseHeaderRow(headerRow);
            log.debug("Headers for sheet '{}': {}", sheetName, headers);

            // Parse data rows
            List<ExcelRowDTO> rows = new ArrayList<>();
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                ExcelRowDTO rowDTO = parseDataRow(row, headers, entityType, rowNum + 1); // +1 for Excel row number
                if (rowDTO != null) {
                    rows.add(rowDTO);
                }
            }

            return ExcelSheetDTO.builder()
                    .sheetName(sheetName)
                    .entityType(entityType)
                    .headers(headers)
                    .rows(rows)
                    .totalRows(rows.size())
                    .build();

        } catch (Exception e) {
            log.error("Error parsing sheet '{}': {}", sheetName, e.getMessage(), e);
            throw new ExcelProcessingException("Failed to parse sheet '" + sheetName + "': " + e.getMessage(), e);
        }
    }

    /**
     * Detect entity type from sheet name
     */
    private ExcelEntityType detectEntityType(String sheetName) {
        String normalizedName = sheetName.toLowerCase().trim();

        // Try exact matches first
        for (ExcelEntityType type : ExcelEntityType.values()) {
            if (type.getSheetName().toLowerCase().equals(normalizedName)) {
                return type;
            }
        }

        // Try partial matches
        if (normalizedName.contains("department")) return ExcelEntityType.DEPARTMENT;
        if (normalizedName.contains("qualification")) return ExcelEntityType.QUALIFICATION;
        if (normalizedName.contains("working") && normalizedName.contains("period")) return ExcelEntityType.WORKING_PERIOD;
        if (normalizedName.contains("squad") && normalizedName.contains("pattern")) return ExcelEntityType.SQUAD_WORKING_PATTERN;
        if (normalizedName.contains("shift")) return ExcelEntityType.SHIFT;
        if (normalizedName.contains("squad")) return ExcelEntityType.SQUAD;
        if (normalizedName.contains("staff") || normalizedName.contains("employee")) return ExcelEntityType.STAFF;
        if (normalizedName.contains("task")) return ExcelEntityType.TASK;
        if (normalizedName.contains("dayoff") || normalizedName.contains("day_off")) return ExcelEntityType.DAY_OFF_RULE;
        if (normalizedName.contains("constraint")) return ExcelEntityType.CONSTRAINT_OVERRIDE;

        return null;
    }

    /**
     * Parse header row and extract column names
     */
    private List<String> parseHeaderRow(Row headerRow) {
        List<String> headers = new ArrayList<>();

        for (int cellNum = 0; cellNum < headerRow.getLastCellNum(); cellNum++) {
            Cell cell = headerRow.getCell(cellNum);
            String header = getCellValueAsString(cell);

            if (StringUtils.hasText(header)) {
                headers.add(header.trim().toUpperCase()); // Normalize to uppercase
            } else {
                headers.add("COLUMN_" + cellNum); // Default name for empty headers
            }
        }

        return headers;
    }

    /**
     * Parse single data row and convert to DTO
     */
    private ExcelRowDTO parseDataRow(Row row, List<String> headers, ExcelEntityType entityType, int excelRowNumber) {
        Map<String, String> cellValues = new HashMap<>();

        // Generate unique row ID
        String rowId = entityType.name() + "_" + excelRowNumber + "_" + System.currentTimeMillis();

        // Parse each cell
        for (int cellNum = 0; cellNum < headers.size() && cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            String header = headers.get(cellNum);
            String value = getCellValueAsString(cell);

            cellValues.put(header, value);
        }

        return ExcelRowDTO.builder()
                .rowId(rowId)
                .rowNumber(excelRowNumber)
                .entityType(entityType)
                .cellValues(cellValues)
                .errors(new ArrayList<>()) // Will be populated during validation
                .canImport(true) // Will be determined during validation
                .build();
    }

    /**
     * Convert Excel cell value to string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    // Handle date/time cells
                    Date date = cell.getDateCellValue();
                    if (date != null) {
                        // Convert to LocalDateTime and format
                        LocalDateTime dateTime = date.toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toLocalDateTime();

                        // Check if it's time-only (date part is 1900-01-01)
                        if (dateTime.toLocalDate().equals(LocalDate.of(1900, 1, 1))) {
                            return dateTime.toLocalTime().toString(); // HH:mm:ss format
                        } else {
                            return dateTime.toString(); // Full datetime format
                        }
                    }
                } else {
                    // Regular number
                    double numValue = cell.getNumericCellValue();
                    // Return as integer if it's a whole number
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    } else {
                        return String.valueOf(numValue);
                    }
                }
                return "";

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case FORMULA:
                try {
                    // Try to get the cached formula result
                    return getCellValueAsString(cell.getCachedFormulaResultType(), cell);
                } catch (Exception e) {
                    log.warn("Error reading formula cell: {}", e.getMessage());
                    return cell.getCellFormula();
                }

            case BLANK:
            case _NONE:
            default:
                return "";
        }
    }

    /**
     * Helper method for formula cells
     */
    private String getCellValueAsString(CellType cellType, Cell cell) {
        switch (cellType) {
            case STRING:
                return cell.getRichStringCellValue().getString();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * Check if row is empty (all cells are blank)
     */
    private boolean isEmptyRow(Row row) {
        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                    StringUtils.hasText(getCellValueAsString(cell))) {
                return false;
            }
        }
        return true;
    }
}
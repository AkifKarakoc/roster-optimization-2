package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.ShiftDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
import com.rosteroptimization.service.excel.ForeignKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShiftExcelParser implements EntityExcelParser<ShiftDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "START_TIME", "END_TIME", "IS_NIGHT_SHIFT",
            "FIXED", "WORKING_PERIOD_NAME", "DESCRIPTION", "ACTIVE"
    };

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER_SHORT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<ExcelRowResult<ShiftDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<ShiftDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<ShiftDTO> result = new ExcelRowResult<>();
            result.setRowNumber(i + 1);

            try {
                ExcelRowData rowData = new ExcelRowData(row, HEADERS);

                // Parse operation
                String operationStr = rowData.getValue("OPERATION").toUpperCase();
                if (operationStr.isEmpty()) {
                    result.addError("OPERATION", "Operation is required");
                    results.add(result);
                    continue;
                }

                ExcelOperation operation;
                try {
                    operation = ExcelOperation.valueOf(operationStr);
                    result.setOperation(operation);
                } catch (IllegalArgumentException e) {
                    result.addError("OPERATION", "Invalid operation: " + operationStr);
                    results.add(result);
                    continue;
                }

                ShiftDTO dto = new ShiftDTO();

                // Handle ID for UPDATE/DELETE
                if (operation == ExcelOperation.UPDATE || operation == ExcelOperation.DELETE) {
                    String idStr = rowData.getValue("ID");
                    if (!idStr.isEmpty()) {
                        try {
                            dto.setId(Long.parseLong(idStr));
                        } catch (NumberFormatException e) {
                            result.addError("ID", "Invalid ID format: " + idStr);
                        }
                    } else {
                        result.addError("ID", "ID is required for " + operation + " operation");
                    }
                }

                // Handle required fields for ADD/UPDATE
                if (operation == ExcelOperation.ADD || operation == ExcelOperation.UPDATE) {
                    // NAME
                    String name = rowData.getValue("NAME");
                    if (!name.isEmpty()) {
                        dto.setName(name);
                    } else {
                        result.addError("NAME", "Name is required for " + operation + " operation");
                    }

                    // START_TIME
                    String startTimeStr = rowData.getValue("START_TIME");
                    if (!startTimeStr.isEmpty()) {
                        try {
                            LocalTime startTime = parseTime(startTimeStr);
                            dto.setStartTime(startTime);
                        } catch (DateTimeParseException e) {
                            result.addError("START_TIME", "Invalid time format: " + startTimeStr);
                        }
                    } else {
                        result.addError("START_TIME", "Start time is required for " + operation + " operation");
                    }

                    // END_TIME
                    String endTimeStr = rowData.getValue("END_TIME");
                    if (!endTimeStr.isEmpty()) {
                        try {
                            LocalTime endTime = parseTime(endTimeStr);
                            dto.setEndTime(endTime);
                        } catch (DateTimeParseException e) {
                            result.addError("END_TIME", "Invalid time format: " + endTimeStr);
                        }
                    } else {
                        result.addError("END_TIME", "End time is required for " + operation + " operation");
                    }

                    // WORKING_PERIOD_NAME (FK Resolution)
                    String workingPeriodName = rowData.getValue("WORKING_PERIOD_NAME");
                    if (!workingPeriodName.isEmpty()) {
                        Long workingPeriodId = foreignKeyResolver.resolveWorkingPeriod(workingPeriodName);
                        if (workingPeriodId != null) {
                            dto.setWorkingPeriodId(workingPeriodId);
                        } else {
                            result.addError("WORKING_PERIOD_NAME", "Working period not found: " + workingPeriodName);
                        }
                    } else {
                        result.addError("WORKING_PERIOD_NAME", "Working period name is required for " + operation + " operation");
                    }
                }

                // Optional boolean fields
                String isNightShiftStr = rowData.getValue("IS_NIGHT_SHIFT");
                if (!isNightShiftStr.isEmpty()) {
                    dto.setIsNightShift(parseBoolean(isNightShiftStr, result, "IS_NIGHT_SHIFT"));
                } else {
                    dto.setIsNightShift(false);
                }

                String fixedStr = rowData.getValue("FIXED");
                if (!fixedStr.isEmpty()) {
                    dto.setFixed(parseBoolean(fixedStr, result, "FIXED"));
                } else {
                    dto.setFixed(true);
                }

                String activeStr = rowData.getValue("ACTIVE");
                if (!activeStr.isEmpty()) {
                    dto.setActive(parseBoolean(activeStr, result, "ACTIVE"));
                } else {
                    dto.setActive(true);
                }

                dto.setDescription(rowData.getValue("DESCRIPTION"));

                result.setData(dto);
                validateRow(result);

            } catch (Exception e) {
                log.error("Error parsing shift row {}: ", i + 1, e);
                result.addError("GENERAL", "Error parsing row: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    private LocalTime parseTime(String timeStr) throws DateTimeParseException {
        timeStr = timeStr.trim();
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalTime.parse(timeStr, TIME_FORMATTER_SHORT);
        }
    }

    private Boolean parseBoolean(String value, ExcelRowResult<ShiftDTO> result, String fieldName) {
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return true;
        } else if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return false;
        } else {
            result.addError(fieldName, "Invalid boolean value: " + value + ". Use true/false or 1/0");
            return false;
        }
    }

    @Override
    public String[] getExpectedHeaders() {
        return HEADERS;
    }

    @Override
    public String getEntityType() {
        return "Shift";
    }

    @Override
    public void validateRow(ExcelRowResult<ShiftDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        ShiftDTO dto = rowResult.getData();

        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            rowResult.addError("DESCRIPTION", "Description cannot exceed 500 characters");
        }

        // Validate time logic
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (dto.getStartTime().equals(dto.getEndTime())) {
                rowResult.addError("TIME", "Start time and end time cannot be the same");
            }
        }
    }
}
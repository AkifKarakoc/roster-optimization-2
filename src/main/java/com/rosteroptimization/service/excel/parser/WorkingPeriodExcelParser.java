package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.WorkingPeriodDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
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
public class WorkingPeriodExcelParser implements EntityExcelParser<WorkingPeriodDTO> {

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "START_TIME", "END_TIME", "DESCRIPTION", "ACTIVE"
    };

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter TIME_FORMATTER_SHORT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public List<ExcelRowResult<WorkingPeriodDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<WorkingPeriodDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<WorkingPeriodDTO> result = new ExcelRowResult<>();
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

                WorkingPeriodDTO dto = new WorkingPeriodDTO();

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
                            result.addError("START_TIME", "Invalid time format: " + startTimeStr + ". Use HH:mm or HH:mm:ss");
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
                            result.addError("END_TIME", "Invalid time format: " + endTimeStr + ". Use HH:mm or HH:mm:ss");
                        }
                    } else {
                        result.addError("END_TIME", "End time is required for " + operation + " operation");
                    }
                }

                // Optional fields
                dto.setDescription(rowData.getValue("DESCRIPTION"));

                String activeStr = rowData.getValue("ACTIVE");
                if (!activeStr.isEmpty()) {
                    if ("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr)) {
                        dto.setActive(true);
                    } else if ("false".equalsIgnoreCase(activeStr) || "0".equals(activeStr)) {
                        dto.setActive(false);
                    } else {
                        result.addError("ACTIVE", "Invalid active value: " + activeStr);
                    }
                } else {
                    dto.setActive(true);
                }

                result.setData(dto);
                validateRow(result);

            } catch (Exception e) {
                log.error("Error parsing working period row {}: ", i + 1, e);
                result.addError("GENERAL", "Error parsing row: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    private LocalTime parseTime(String timeStr) throws DateTimeParseException {
        timeStr = timeStr.trim();

        // Try HH:mm:ss format first
        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try HH:mm format
            return LocalTime.parse(timeStr, TIME_FORMATTER_SHORT);
        }
    }

    @Override
    public String[] getExpectedHeaders() {
        return HEADERS;
    }

    @Override
    public String getEntityType() {
        return "WorkingPeriod";
    }

    @Override
    public void validateRow(ExcelRowResult<WorkingPeriodDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        WorkingPeriodDTO dto = rowResult.getData();

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
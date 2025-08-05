package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.SquadWorkingPatternDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SquadWorkingPatternExcelParser implements EntityExcelParser<SquadWorkingPatternDTO> {

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "SHIFT_PATTERN", "CYCLE_LENGTH", "DESCRIPTION", "ACTIVE"
    };

    @Override
    public List<ExcelRowResult<SquadWorkingPatternDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<SquadWorkingPatternDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<SquadWorkingPatternDTO> result = new ExcelRowResult<>();
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

                SquadWorkingPatternDTO dto = new SquadWorkingPatternDTO();

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

                    // SHIFT_PATTERN
                    String shiftPattern = rowData.getValue("SHIFT_PATTERN");
                    if (!shiftPattern.isEmpty()) {
                        dto.setShiftPattern(shiftPattern);
                    } else {
                        result.addError("SHIFT_PATTERN", "Shift pattern is required for " + operation + " operation");
                    }

                    // CYCLE_LENGTH
                    String cycleLengthStr = rowData.getValue("CYCLE_LENGTH");
                    if (!cycleLengthStr.isEmpty()) {
                        try {
                            int cycleLength = Integer.parseInt(cycleLengthStr);
                            if (cycleLength < 1 || cycleLength > 365) {
                                result.addError("CYCLE_LENGTH", "Cycle length must be between 1 and 365");
                            } else {
                                dto.setCycleLength(cycleLength);
                            }
                        } catch (NumberFormatException e) {
                            result.addError("CYCLE_LENGTH", "Invalid cycle length format: " + cycleLengthStr);
                        }
                    } else {
                        result.addError("CYCLE_LENGTH", "Cycle length is required for " + operation + " operation");
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
                log.error("Error parsing squad working pattern row {}: ", i + 1, e);
                result.addError("GENERAL", "Error parsing row: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    @Override
    public String[] getExpectedHeaders() {
        return HEADERS;
    }

    @Override
    public String getEntityType() {
        return "SquadWorkingPattern";
    }

    @Override
    public void validateRow(ExcelRowResult<SquadWorkingPatternDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        SquadWorkingPatternDTO dto = rowResult.getData();

        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            rowResult.addError("DESCRIPTION", "Description cannot exceed 500 characters");
        }

        if (dto.getShiftPattern() != null && dto.getShiftPattern().length() > 1000) {
            rowResult.addError("SHIFT_PATTERN", "Shift pattern cannot exceed 1000 characters");
        }

        // Validate shift pattern format
        if (dto.getShiftPattern() != null) {
            validateShiftPattern(dto.getShiftPattern(), rowResult);
        }
    }

    private void validateShiftPattern(String pattern, ExcelRowResult<SquadWorkingPatternDTO> rowResult) {
        if (pattern.trim().isEmpty()) {
            rowResult.addError("SHIFT_PATTERN", "Shift pattern cannot be empty");
            return;
        }

        String[] elements = pattern.split(",");
        for (String element : elements) {
            element = element.trim();
            if (element.isEmpty()) {
                rowResult.addError("SHIFT_PATTERN", "Shift pattern contains empty elements");
                return;
            }

            // Check if it's a valid pattern element (shift name or "DayOff")
            if (!element.equalsIgnoreCase("DayOff") && !isValidShiftName(element)) {
                // Just log warning, don't fail validation since we can't validate shift names without FK resolver here
                log.warn("Potentially invalid shift name in pattern: {}", element);
            }
        }
    }

    private boolean isValidShiftName(String shiftName) {
        // Basic validation - more detailed validation will be done with FK resolver
        return shiftName.matches("^[a-zA-Z0-9_\\-\\s]+$");
    }
}
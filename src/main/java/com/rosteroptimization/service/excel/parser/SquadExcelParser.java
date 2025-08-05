package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.SquadDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
import com.rosteroptimization.service.excel.ForeignKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SquadExcelParser implements EntityExcelParser<SquadDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "START_DATE", "SQUAD_WORKING_PATTERN_NAME", "DESCRIPTION", "ACTIVE"
    };

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<ExcelRowResult<SquadDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<SquadDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<SquadDTO> result = new ExcelRowResult<>();
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

                SquadDTO dto = new SquadDTO();

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

                    // START_DATE
                    String startDateStr = rowData.getValue("START_DATE");
                    if (!startDateStr.isEmpty()) {
                        try {
                            LocalDate startDate = LocalDate.parse(startDateStr, DATE_FORMATTER);
                            dto.setStartDate(startDate);
                        } catch (DateTimeParseException e) {
                            result.addError("START_DATE", "Invalid date format: " + startDateStr + ". Use yyyy-MM-dd");
                        }
                    } else {
                        result.addError("START_DATE", "Start date is required for " + operation + " operation");
                    }

                    // SQUAD_WORKING_PATTERN_NAME (FK Resolution)
                    String patternName = rowData.getValue("SQUAD_WORKING_PATTERN_NAME");
                    if (!patternName.isEmpty()) {
                        Long patternId = foreignKeyResolver.resolveSquadWorkingPattern(patternName);
                        if (patternId != null) {
                            dto.setSquadWorkingPatternId(patternId);
                        } else {
                            result.addError("SQUAD_WORKING_PATTERN_NAME", "Squad working pattern not found: " + patternName);
                        }
                    } else {
                        result.addError("SQUAD_WORKING_PATTERN_NAME", "Squad working pattern name is required for " + operation + " operation");
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
                log.error("Error parsing squad row {}: ", i + 1, e);
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
        return "Squad";
    }

    @Override
    public void validateRow(ExcelRowResult<SquadDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        SquadDTO dto = rowResult.getData();

        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            rowResult.addError("DESCRIPTION", "Description cannot exceed 500 characters");
        }

        // Validate start date is not in the future (optional business rule)
        if (dto.getStartDate() != null && dto.getStartDate().isAfter(LocalDate.now().plusDays(30))) {
            rowResult.addError("START_DATE", "Start date cannot be more than 30 days in the future");
        }
    }
}
package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.DepartmentDTO;
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
public class DepartmentExcelParser implements EntityExcelParser<DepartmentDTO> {

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "DESCRIPTION", "ACTIVE"
    };

    @Override
    public List<ExcelRowResult<DepartmentDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<DepartmentDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        // Skip header row, start from row 1
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<DepartmentDTO> result = new ExcelRowResult<>();
            result.setRowNumber(i + 1); // 1-based for user display

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
                    result.addError("OPERATION", "Invalid operation: " + operationStr + ". Must be ADD, UPDATE, or DELETE");
                    results.add(result);
                    continue;
                }

                // Create DTO
                DepartmentDTO dto = new DepartmentDTO();

                // Handle ID for UPDATE/DELETE operations
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

                // Handle NAME (required for ADD and UPDATE)
                if (operation == ExcelOperation.ADD || operation == ExcelOperation.UPDATE) {
                    String name = rowData.getValue("NAME");
                    if (!name.isEmpty()) {
                        dto.setName(name);
                    } else {
                        result.addError("NAME", "Name is required for " + operation + " operation");
                    }
                }

                // Handle optional fields
                dto.setDescription(rowData.getValue("DESCRIPTION"));

                String activeStr = rowData.getValue("ACTIVE");
                if (!activeStr.isEmpty()) {
                    if ("true".equalsIgnoreCase(activeStr) || "1".equals(activeStr)) {
                        dto.setActive(true);
                    } else if ("false".equalsIgnoreCase(activeStr) || "0".equals(activeStr)) {
                        dto.setActive(false);
                    } else {
                        result.addError("ACTIVE", "Invalid active value: " + activeStr + ". Must be true/false or 1/0");
                    }
                } else {
                    dto.setActive(true); // Default value
                }

                result.setData(dto);
                validateRow(result);

            } catch (Exception e) {
                log.error("Error parsing row {}: ", i + 1, e);
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
        return "Department";
    }

    @Override
    public void validateRow(ExcelRowResult<DepartmentDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return; // Skip validation if already has errors
        }

        DepartmentDTO dto = rowResult.getData();

        // Validate name length
        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        // Validate description length
        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            rowResult.addError("DESCRIPTION", "Description cannot exceed 500 characters");
        }

        // Additional business validations can be added here
    }
}
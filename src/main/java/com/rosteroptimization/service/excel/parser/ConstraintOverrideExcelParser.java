package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.ConstraintOverrideDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
import com.rosteroptimization.service.excel.ForeignKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConstraintOverrideExcelParser implements EntityExcelParser<ConstraintOverrideDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "OVERRIDE_VALUE", "STAFF_REGISTRATION_CODE", "CONSTRAINT_NAME"
    };

    @Override
    public List<ExcelRowResult<ConstraintOverrideDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<ConstraintOverrideDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<ConstraintOverrideDTO> result = new ExcelRowResult<>();
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

                ConstraintOverrideDTO dto = new ConstraintOverrideDTO();

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
                    // OVERRIDE_VALUE
                    String overrideValue = rowData.getValue("OVERRIDE_VALUE");
                    if (!overrideValue.isEmpty()) {
                        dto.setOverrideValue(overrideValue);
                    } else {
                        result.addError("OVERRIDE_VALUE", "Override value is required for " + operation + " operation");
                    }

                    // STAFF_REGISTRATION_CODE (FK Resolution)
                    String staffRegistrationCode = rowData.getValue("STAFF_REGISTRATION_CODE");
                    if (!staffRegistrationCode.isEmpty()) {
                        Long staffId = foreignKeyResolver.resolveStaff(staffRegistrationCode);
                        if (staffId != null) {
                            dto.setStaffId(staffId);
                        } else {
                            result.addError("STAFF_REGISTRATION_CODE", "Staff not found: " + staffRegistrationCode);
                        }
                    } else {
                        result.addError("STAFF_REGISTRATION_CODE", "Staff registration code is required for " + operation + " operation");
                    }

                    // CONSTRAINT_NAME (FK Resolution)
                    String constraintName = rowData.getValue("CONSTRAINT_NAME");
                    if (!constraintName.isEmpty()) {
                        Long constraintId = foreignKeyResolver.resolveConstraint(constraintName);
                        if (constraintId != null) {
                            dto.setConstraintId(constraintId);
                        } else {
                            result.addError("CONSTRAINT_NAME", "Constraint not found: " + constraintName);
                        }
                    } else {
                        result.addError("CONSTRAINT_NAME", "Constraint name is required for " + operation + " operation");
                    }
                }

                result.setData(dto);
                validateRow(result);

            } catch (Exception e) {
                log.error("Error parsing constraint override row {}: ", i + 1, e);
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
        return "ConstraintOverride";
    }

    @Override
    public void validateRow(ExcelRowResult<ConstraintOverrideDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        ConstraintOverrideDTO dto = rowResult.getData();

        // Field length validations
        if (dto.getOverrideValue() != null && dto.getOverrideValue().length() > 100) {
            rowResult.addError("OVERRIDE_VALUE", "Override value cannot exceed 100 characters");
        }

        // Basic value format validation (could be enhanced based on constraint types)
        if (dto.getOverrideValue() != null) {
            String value = dto.getOverrideValue().trim();

            // Check if it's a valid boolean
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                // Valid boolean
            }
            // Check if it's a valid number
            else if (value.matches("^-?\\d+(\\.\\d+)?$")) {
                // Valid number
            }
            // Check if it's a valid time format
            else if (value.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
                // Valid time format
            }
            // Otherwise, treat as string (should be validated against constraint type in business logic)
            else {
                log.debug("Override value '{}' will be treated as string for row {}",
                        value, rowResult.getRowNumber());
            }
        }
    }
}
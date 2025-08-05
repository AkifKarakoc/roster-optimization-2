package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.DayOffRuleDTO;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayOffRuleExcelParser implements EntityExcelParser<DayOffRuleDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "WORKING_DAYS", "OFF_DAYS", "FIXED_OFF_DAYS", "STAFF_REGISTRATION_CODE"
    };

    private static final Set<String> VALID_DAYS = Set.of(
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
    );

    @Override
    public List<ExcelRowResult<DayOffRuleDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<DayOffRuleDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<DayOffRuleDTO> result = new ExcelRowResult<>();
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

                DayOffRuleDTO dto = new DayOffRuleDTO();

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
                    // WORKING_DAYS
                    String workingDaysStr = rowData.getValue("WORKING_DAYS");
                    if (!workingDaysStr.isEmpty()) {
                        try {
                            int workingDays = Integer.parseInt(workingDaysStr);
                            if (workingDays < 1 || workingDays > 14) {
                                result.addError("WORKING_DAYS", "Working days must be between 1 and 14");
                            } else {
                                dto.setWorkingDays(workingDays);
                            }
                        } catch (NumberFormatException e) {
                            result.addError("WORKING_DAYS", "Invalid working days format: " + workingDaysStr);
                        }
                    } else {
                        result.addError("WORKING_DAYS", "Working days is required for " + operation + " operation");
                    }

                    // OFF_DAYS
                    String offDaysStr = rowData.getValue("OFF_DAYS");
                    if (!offDaysStr.isEmpty()) {
                        try {
                            int offDays = Integer.parseInt(offDaysStr);
                            if (offDays < 1 || offDays > 7) {
                                result.addError("OFF_DAYS", "Off days must be between 1 and 7");
                            } else {
                                dto.setOffDays(offDays);
                            }
                        } catch (NumberFormatException e) {
                            result.addError("OFF_DAYS", "Invalid off days format: " + offDaysStr);
                        }
                    } else {
                        result.addError("OFF_DAYS", "Off days is required for " + operation + " operation");
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
                }

                // Optional field: FIXED_OFF_DAYS
                String fixedOffDays = rowData.getValue("FIXED_OFF_DAYS");
                if (!fixedOffDays.isEmpty() && !fixedOffDays.equalsIgnoreCase("null")) {
                    if (validateFixedOffDays(fixedOffDays, result)) {
                        dto.setFixedOffDays(fixedOffDays.toUpperCase());
                    }
                }

                result.setData(dto);
                validateRow(result);

            } catch (Exception e) {
                log.error("Error parsing day off rule row {}: ", i + 1, e);
                result.addError("GENERAL", "Error parsing row: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    private boolean validateFixedOffDays(String fixedOffDays, ExcelRowResult<DayOffRuleDTO> result) {
        if (fixedOffDays == null || fixedOffDays.trim().isEmpty()) {
            return true; // Optional field
        }

        String[] days = fixedOffDays.toUpperCase().split(",");
        for (String day : days) {
            day = day.trim();
            if (!VALID_DAYS.contains(day)) {
                result.addError("FIXED_OFF_DAYS", "Invalid day name: " + day + ". Valid days: " + VALID_DAYS);
                return false;
            }
        }

        // Check for duplicates
        Set<String> uniqueDays = Arrays.stream(days)
                .map(String::trim)
                .collect(Collectors.toSet());

        if (uniqueDays.size() != days.length) {
            result.addError("FIXED_OFF_DAYS", "Duplicate days found in fixed off days");
            return false;
        }

        // Check maximum 7 days
        if (uniqueDays.size() > 7) {
            result.addError("FIXED_OFF_DAYS", "Cannot have more than 7 fixed off days");
            return false;
        }

        return true;
    }

    @Override
    public String[] getExpectedHeaders() {
        return HEADERS;
    }

    @Override
    public String getEntityType() {
        return "DayOffRule";
    }

    @Override
    public void validateRow(ExcelRowResult<DayOffRuleDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        DayOffRuleDTO dto = rowResult.getData();

        // Business logic validation
        if (dto.getWorkingDays() != null && dto.getOffDays() != null) {
            // Total cycle should be reasonable
            int totalCycle = dto.getWorkingDays() + dto.getOffDays();
            if (totalCycle > 21) { // Max 3 weeks cycle
                rowResult.addError("CYCLE", "Total cycle (working days + off days) cannot exceed 21 days");
            }

            // Off days should not be more than working days in most cases
            if (dto.getOffDays() > dto.getWorkingDays()) {
                log.warn("Off days ({}) are more than working days ({}) for row {}",
                        dto.getOffDays(), dto.getWorkingDays(), rowResult.getRowNumber());
            }
        }

        // Validate fixed off days length
        if (dto.getFixedOffDays() != null && dto.getFixedOffDays().length() > 100) {
            rowResult.addError("FIXED_OFF_DAYS", "Fixed off days cannot exceed 100 characters");
        }

        // If fixed off days are specified, validate they make sense with off days count
        if (dto.getFixedOffDays() != null && !dto.getFixedOffDays().isEmpty() && dto.getOffDays() != null) {
            String[] fixedDays = dto.getFixedOffDays().split(",");
            if (fixedDays.length < dto.getOffDays()) {
                log.warn("Fixed off days count ({}) is less than required off days ({}) for row {}",
                        fixedDays.length, dto.getOffDays(), rowResult.getRowNumber());
            }
        }
    }
}
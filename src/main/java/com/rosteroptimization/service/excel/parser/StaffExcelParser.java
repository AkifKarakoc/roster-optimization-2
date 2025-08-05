package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.StaffDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class StaffExcelParser implements EntityExcelParser<StaffDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "SURNAME", "REGISTRATION_CODE", "EMAIL", "PHONE",
            "DEPARTMENT_NAME", "SQUAD_NAME", "QUALIFICATIONS", "ACTIVE"
    };

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[0-9\\s\\-\\(\\)]*$"
    );

    private static final Pattern REGISTRATION_CODE_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_-]+$"
    );

    @Override
    public List<ExcelRowResult<StaffDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<StaffDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<StaffDTO> result = new ExcelRowResult<>();
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

                StaffDTO dto = new StaffDTO();

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

                    // SURNAME
                    String surname = rowData.getValue("SURNAME");
                    if (!surname.isEmpty()) {
                        dto.setSurname(surname);
                    } else {
                        result.addError("SURNAME", "Surname is required for " + operation + " operation");
                    }

                    // REGISTRATION_CODE
                    String registrationCode = rowData.getValue("REGISTRATION_CODE");
                    if (!registrationCode.isEmpty()) {
                        if (REGISTRATION_CODE_PATTERN.matcher(registrationCode).matches()) {
                            dto.setRegistrationCode(registrationCode);
                        } else {
                            result.addError("REGISTRATION_CODE", "Registration code can only contain letters, numbers, underscores and hyphens");
                        }
                    } else {
                        result.addError("REGISTRATION_CODE", "Registration code is required for " + operation + " operation");
                    }

                    // DEPARTMENT_NAME (FK Resolution)
                    String departmentName = rowData.getValue("DEPARTMENT_NAME");
                    if (!departmentName.isEmpty()) {
                        Long departmentId = foreignKeyResolver.resolveDepartment(departmentName);
                        if (departmentId != null) {
                            dto.setDepartmentId(departmentId);
                        } else {
                            result.addError("DEPARTMENT_NAME", "Department not found: " + departmentName);
                        }
                    } else {
                        result.addError("DEPARTMENT_NAME", "Department name is required for " + operation + " operation");
                    }

                    // SQUAD_NAME (FK Resolution)
                    String squadName = rowData.getValue("SQUAD_NAME");
                    if (!squadName.isEmpty()) {
                        Long squadId = foreignKeyResolver.resolveSquad(squadName);
                        if (squadId != null) {
                            dto.setSquadId(squadId);
                        } else {
                            result.addError("SQUAD_NAME", "Squad not found: " + squadName);
                        }
                    } else {
                        result.addError("SQUAD_NAME", "Squad name is required for " + operation + " operation");
                    }

                    // QUALIFICATIONS (FK Resolution - comma separated)
                    String qualifications = rowData.getValue("QUALIFICATIONS");
                    if (!qualifications.isEmpty()) {
                        Set<Long> qualificationIds = foreignKeyResolver.resolveQualifications(qualifications);
                        if (!qualificationIds.isEmpty()) {
                            dto.setQualificationIds(qualificationIds);
                        } else {
                            result.addError("QUALIFICATIONS", "No valid qualifications found in: " + qualifications);
                        }
                    }
                }

                // Optional fields
                String email = rowData.getValue("EMAIL");
                if (!email.isEmpty()) {
                    if (EMAIL_PATTERN.matcher(email).matches()) {
                        dto.setEmail(email);
                    } else {
                        result.addError("EMAIL", "Invalid email format: " + email);
                    }
                }

                String phone = rowData.getValue("PHONE");
                if (!phone.isEmpty()) {
                    if (PHONE_PATTERN.matcher(phone).matches()) {
                        dto.setPhone(phone);
                    } else {
                        result.addError("PHONE", "Invalid phone format: " + phone);
                    }
                }

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
                log.error("Error parsing staff row {}: ", i + 1, e);
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
        return "Staff";
    }

    @Override
    public void validateRow(ExcelRowResult<StaffDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        StaffDTO dto = rowResult.getData();

        // Field length validations
        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        if (dto.getSurname() != null && dto.getSurname().length() > 100) {
            rowResult.addError("SURNAME", "Surname cannot exceed 100 characters");
        }

        if (dto.getRegistrationCode() != null && dto.getRegistrationCode().length() > 50) {
            rowResult.addError("REGISTRATION_CODE", "Registration code cannot exceed 50 characters");
        }

        if (dto.getEmail() != null && dto.getEmail().length() > 100) {
            rowResult.addError("EMAIL", "Email cannot exceed 100 characters");
        }

        if (dto.getPhone() != null && dto.getPhone().length() > 20) {
            rowResult.addError("PHONE", "Phone cannot exceed 20 characters");
        }
    }
}
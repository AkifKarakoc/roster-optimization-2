package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.TaskDTO;
import com.rosteroptimization.dto.excel.ExcelRowData;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.enums.ExcelOperation;
import com.rosteroptimization.service.excel.ForeignKeyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskExcelParser implements EntityExcelParser<TaskDTO> {

    private final ForeignKeyResolver foreignKeyResolver;

    private static final String[] HEADERS = {
            "OPERATION", "ID", "NAME", "START_TIME", "END_TIME", "PRIORITY",
            "DEPARTMENT_NAME", "REQUIRED_QUALIFICATIONS", "DESCRIPTION", "ACTIVE"
    };

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER_ALT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<ExcelRowResult<TaskDTO>> parseRows(Sheet sheet) {
        List<ExcelRowResult<TaskDTO>> results = new ArrayList<>();

        if (sheet.getLastRowNum() < 1) {
            log.warn("No data rows found in sheet");
            return results;
        }

        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            ExcelRowResult<TaskDTO> result = new ExcelRowResult<>();
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

                TaskDTO dto = new TaskDTO();

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
                            LocalDateTime startTime = parseDateTime(startTimeStr);
                            dto.setStartTime(startTime);
                        } catch (DateTimeParseException e) {
                            result.addError("START_TIME", "Invalid datetime format: " + startTimeStr + ". Use yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd HH:mm:ss");
                        }
                    } else {
                        result.addError("START_TIME", "Start time is required for " + operation + " operation");
                    }

                    // END_TIME
                    String endTimeStr = rowData.getValue("END_TIME");
                    if (!endTimeStr.isEmpty()) {
                        try {
                            LocalDateTime endTime = parseDateTime(endTimeStr);
                            dto.setEndTime(endTime);
                        } catch (DateTimeParseException e) {
                            result.addError("END_TIME", "Invalid datetime format: " + endTimeStr + ". Use yyyy-MM-dd'T'HH:mm:ss or yyyy-MM-dd HH:mm:ss");
                        }
                    } else {
                        result.addError("END_TIME", "End time is required for " + operation + " operation");
                    }

                    // PRIORITY
                    String priorityStr = rowData.getValue("PRIORITY");
                    if (!priorityStr.isEmpty()) {
                        try {
                            int priority = Integer.parseInt(priorityStr);
                            if (priority < 1 || priority > 10) {
                                result.addError("PRIORITY", "Priority must be between 1 and 10");
                            } else {
                                dto.setPriority(priority);
                            }
                        } catch (NumberFormatException e) {
                            result.addError("PRIORITY", "Invalid priority format: " + priorityStr);
                        }
                    } else {
                        result.addError("PRIORITY", "Priority is required for " + operation + " operation");
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

                    // REQUIRED_QUALIFICATIONS (FK Resolution - comma separated)
                    String requiredQualifications = rowData.getValue("REQUIRED_QUALIFICATIONS");
                    if (!requiredQualifications.isEmpty()) {
                        Set<Long> qualificationIds = foreignKeyResolver.resolveQualifications(requiredQualifications);
                        if (!qualificationIds.isEmpty()) {
                            dto.setRequiredQualificationIds(qualificationIds);
                        } else {
                            // Don't error out if no qualifications found - some tasks might not require qualifications
                            log.warn("No valid qualifications found for task in row {}: {}", result.getRowNumber(), requiredQualifications);
                        }
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
                log.error("Error parsing task row {}: ", i + 1, e);
                result.addError("GENERAL", "Error parsing row: " + e.getMessage());
            }

            results.add(result);
        }

        return results;
    }

    private LocalDateTime parseDateTime(String dateTimeStr) throws DateTimeParseException {
        dateTimeStr = dateTimeStr.trim();
        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER_ALT);
        }
    }

    @Override
    public String[] getExpectedHeaders() {
        return HEADERS;
    }

    @Override
    public String getEntityType() {
        return "Task";
    }

    @Override
    public void validateRow(ExcelRowResult<TaskDTO> rowResult) {
        if (rowResult.hasErrors()) {
            return;
        }

        TaskDTO dto = rowResult.getData();

        // Field length validations
        if (dto.getName() != null && dto.getName().length() > 100) {
            rowResult.addError("NAME", "Name cannot exceed 100 characters");
        }

        if (dto.getDescription() != null && dto.getDescription().length() > 500) {
            rowResult.addError("DESCRIPTION", "Description cannot exceed 500 characters");
        }

        // Business logic validations
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            if (!dto.getStartTime().isBefore(dto.getEndTime())) {
                rowResult.addError("TIME", "Start time must be before end time");
            }

            // Validate task duration (optional business rule - max 24 hours)
            if (dto.getStartTime().plusHours(24).isBefore(dto.getEndTime())) {
                rowResult.addError("TIME", "Task duration cannot exceed 24 hours");
            }
        }

        // Priority validation is already done in parsing phase

        // Validate task is not too far in the past
        if (dto.getStartTime() != null && dto.getStartTime().isBefore(LocalDateTime.now().minusDays(365))) {
            rowResult.addError("START_TIME", "Task start time cannot be more than 1 year in the past");
        }
    }
}
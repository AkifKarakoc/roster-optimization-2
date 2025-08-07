package com.rosteroptimization.service.excel;

import com.rosteroptimization.service.excel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelValidationService {

    private final ExcelForeignKeyResolver foreignKeyResolver;

    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-\\(\\)]*$");
    private static final Pattern REGISTRATION_CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Validate all sheets and create preview with errors
     */
    public ExcelPreviewDTO validateAndCreatePreview(List<ExcelSheetDTO> sheets) {
        log.info("Starting validation for {} sheets", sheets.size());
        long startTime = System.currentTimeMillis();

        // Create preview DTO
        ExcelPreviewDTO preview = ExcelPreviewDTO.builder()
                .fileName("uploaded_file.xlsx") // Will be set by caller
                .processedAt(LocalDateTime.now())
                .sheets(sheets)
                .entityTypeCounts(new HashMap<>())
                .processingMessages(new ArrayList<>())
                .build();

        // Start batch validation for foreign key resolution
        foreignKeyResolver.startBatchValidation();

        try {
            // First pass: Collect all entities that will be created/updated
            collectBatchEntities(sheets);

            // Second pass: Validate each sheet with batch context
            for (ExcelSheetDTO sheet : sheets) {
                validateSheet(sheet);
            }

            // Update overall statistics
            preview.updateStatistics();
            preview.setProcessingTimeMs(System.currentTimeMillis() - startTime);

            log.info("Validation completed in {}ms. Total rows: {}, Errors: {}, Warnings: {}",
                    preview.getProcessingTimeMs(), preview.getTotalRows(),
                    preview.getErrorRows(), preview.getWarningRows());

            return preview;
        } finally {
            // Clean up batch context
            foreignKeyResolver.clearBatchContext();
        }
    }

    /**
     * Validate single sheet
     */
    private void validateSheet(ExcelSheetDTO sheet) {
        if (sheet.getEntityType() == null) {
            log.warn("Unknown entity type for sheet: {}", sheet.getSheetName());
            return;
        }

        // Validate each row
        for (ExcelRowDTO row : sheet.getRows()) {
            validateRow(row, sheet.getEntityType());
        }

        // Update sheet statistics
        sheet.updateStatistics();
    }

    /**
     * Collect all entities that will be created/updated in this batch
     */
    private void collectBatchEntities(List<ExcelSheetDTO> sheets) {
        for (ExcelSheetDTO sheet : sheets) {
            if (sheet.getEntityType() == null) continue;
            
            for (ExcelRowDTO row : sheet.getRows()) {
                // Operation henüz set edilmemiş olabilir, cell'den al
                EntityOperation operation = row.getOperation();
                if (operation == null) {
                    operation = row.getOperationFromCell();
                }
                
                if (operation == EntityOperation.ADD) {
                    foreignKeyResolver.addToBatch(sheet.getEntityType(), row);
                }
            }
        }
    }

    /**
     * Validate single row based on entity type
     */
    private void validateRow(ExcelRowDTO row, ExcelEntityType entityType) {
        // Clear existing errors
        row.getErrors().clear();

        // 1. Parse and validate operation
        validateOperation(row);

        // 2. Entity-specific validation
        switch (entityType) {
            case DEPARTMENT:
                validateDepartmentRow(row);
                break;
            case QUALIFICATION:
                validateQualificationRow(row);
                break;
            case WORKING_PERIOD:
                validateWorkingPeriodRow(row);
                break;
            case SQUAD_WORKING_PATTERN:
                validateSquadWorkingPatternRow(row);
                break;
            case SHIFT:
                validateShiftRow(row);
                break;
            case SQUAD:
                validateSquadRow(row);
                break;
            case STAFF:
                validateStaffRow(row);
                break;
            case TASK:
                validateTaskRow(row);
                break;
            case DAY_OFF_RULE:
                validateDayOffRuleRow(row);
                break;
            case CONSTRAINT_OVERRIDE:
                validateConstraintOverrideRow(row);
                break;
        }

        // 3. Update canImport flag based on errors
        row.setCanImport(!row.hasBlockingErrors());
    }

    /**
     * Validate operation column
     */
    private void validateOperation(ExcelRowDTO row) {
        try {
            EntityOperation operation = row.getOperationFromCell();
            row.setOperation(operation);

            // Validate operation requirements
            if (operation.requiresExistingEntity()) {
                ParsedEntityId parsedId = row.parseMainEntityId();
                if (!parsedId.hasId() && !parsedId.hasName()) {
                    row.addError(ExcelErrorDTO.error("OPERATION",
                            operation + " operation requires entity ID or name"));
                }
            }

        } catch (IllegalArgumentException e) {
            row.addError(ExcelErrorDTO.error("OPERATION", "Invalid operation: " + e.getMessage()));
            row.setOperation(EntityOperation.ADD); // Default fallback
        }
    }

    /**
     * Validate Department row
     */
    private void validateDepartmentRow(ExcelRowDTO row) {
        // Required fields
        validateRequiredField(row, "NAME", "Department name is required");

        // Name length
        validateStringLength(row, "NAME", 100, "Department name");

        // Description length (optional)
        validateStringLength(row, "DESCRIPTION", 500, "Department description");

        // Foreign key validations (none for Department - it's a base entity)

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, "NAME", "Department");
        }
    }

    /**
     * Validate Qualification row
     */
    private void validateQualificationRow(ExcelRowDTO row) {
        // Required fields
        validateRequiredField(row, "NAME", "Qualification name is required");

        // Name length
        validateStringLength(row, "NAME", 100, "Qualification name");

        // Description length (optional)
        validateStringLength(row, "DESCRIPTION", 500, "Qualification description");

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, "NAME", "Qualification");
        }
    }

    /**
     * Validate WorkingPeriod row
     */
    private void validateWorkingPeriodRow(ExcelRowDTO row) {
        // Required fields - try multiple column name variations
        validateRequiredField(row, getColumnName(row, "NAME"), "Working period name is required");
        validateRequiredField(row, getColumnName(row, "START_TIME", "START TİME"), "Start time is required");
        validateRequiredField(row, getColumnName(row, "END_TIME", "END TİME"), "End time is required");

        // Name length
        validateStringLength(row, getColumnName(row, "NAME"), 100, "Working period name");

        // Time format validation
        LocalTime startTime = validateTimeFormat(row, getColumnName(row, "START_TIME", "START TİME"));
        LocalTime endTime = validateTimeFormat(row, getColumnName(row, "END_TIME", "END TİME"));

        // Time logic validation
        if (startTime != null && endTime != null) {
            if (startTime.equals(endTime)) {
                row.addError(ExcelErrorDTO.error(getColumnName(row, "END_TIME", "END TİME"),
                        "Start time and end time cannot be the same"));
            }
        }

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, getColumnName(row, "NAME"), "WorkingPeriod");
        }
    }

    /**
     * Validate SquadWorkingPattern row
     */
    private void validateSquadWorkingPatternRow(ExcelRowDTO row) {
        // Required fields with multiple column name variations
        validateRequiredField(row, getColumnName(row, "NAME"), "Squad working pattern name is required");
        validateRequiredField(row, getColumnName(row, "SHIFT_PATTERN", "PATTERN"), "Shift pattern is required");
        validateRequiredField(row, getColumnName(row, "CYCLE_LENGTH", "CYCLE LENGTH"), "Cycle length is required");

        // Name length
        validateStringLength(row, getColumnName(row, "NAME"), 100, "Squad working pattern name");

        // Pattern length
        validateStringLength(row, getColumnName(row, "SHIFT_PATTERN", "PATTERN"), 1000, "Shift pattern");

        // Cycle length validation
        Integer cycleLength = validateIntegerRange(row, getColumnName(row, "CYCLE_LENGTH", "CYCLE LENGTH"), 1, 365, "Cycle length");

        // Pattern format validation - basic check
        String pattern = row.getCellValue(getColumnName(row, "SHIFT_PATTERN", "PATTERN"));
        if (StringUtils.hasText(pattern) && cycleLength != null) {
            String[] patternItems = pattern.split(",");
            if (patternItems.length != cycleLength) {
                row.addError(ExcelErrorDTO.warning(getColumnName(row, "SHIFT_PATTERN", "PATTERN"),
                        "Pattern items count (" + patternItems.length +
                                ") doesn't match cycle length (" + cycleLength + ")"));
            }
        }

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, getColumnName(row, "NAME"), "SquadWorkingPattern");
        }
    }

    /**
     * Validate Shift row
     */
    private void validateShiftRow(ExcelRowDTO row) {
        // Required fields with column name variations
        validateRequiredField(row, getColumnName(row, "NAME"), "Shift name is required");
        validateRequiredField(row, getColumnName(row, "START_TIME", "START TİME"), "Start time is required");
        validateRequiredField(row, getColumnName(row, "END_TIME", "END TİME"), "End time is required");
        validateRequiredField(row, getColumnName(row, "WORKING_PERIOD_NAME", "WORKİNG PERİOD ID"), "Working period is required");

        // Name length
        validateStringLength(row, getColumnName(row, "NAME"), 100, "Shift name");

        // Time format validation
        LocalTime startTime = validateTimeFormat(row, getColumnName(row, "START_TIME", "START TİME"));
        LocalTime endTime = validateTimeFormat(row, getColumnName(row, "END_TIME", "END TİME"));

        // Time logic validation
        if (startTime != null && endTime != null) {
            if (startTime.equals(endTime)) {
                row.addError(ExcelErrorDTO.error(getColumnName(row, "END_TIME", "END TİME"),
                        "Start time and end time cannot be the same"));
            }
        }

        // Boolean validation
        validateBooleanField(row, getColumnName(row, "IS_NIGHT_SHIFT"));
        validateBooleanField(row, getColumnName(row, "FIXED", "FİXED"));

        // Foreign key validations
        validateForeignKey(row, getColumnName(row, "WORKING_PERIOD_NAME", "WORKİNG PERİOD ID"), "WorkingPeriod");

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, getColumnName(row, "NAME"), "Shift");
        }
    }

    /**
     * Validate Squad row
     */
    private void validateSquadRow(ExcelRowDTO row) {
        // Required fields with column name variations
        validateRequiredField(row, getColumnName(row, "NAME"), "Squad name is required");
        validateRequiredField(row, getColumnName(row, "START_DATE", "START DATE"), "Start date is required");
        validateRequiredField(row, getColumnName(row, "SQUAD_WORKING_PATTERN_NAME", "PATTERN ID"), "Squad working pattern is required");

        // Name length
        validateStringLength(row, getColumnName(row, "NAME"), 100, "Squad name");

        // Date format validation
        LocalDate startDate = validateDateFormat(row, getColumnName(row, "START_DATE", "START DATE"));

        // Date logic validation
        if (startDate != null) {
            LocalDate maxFutureDate = LocalDate.now().plusYears(1);
            if (startDate.isAfter(maxFutureDate)) {
                row.addError(ExcelErrorDTO.warning(getColumnName(row, "START_DATE", "START DATE"),
                        "Start date is more than 1 year in the future"));
            }
        }

        // Foreign key validations
        validateForeignKey(row, getColumnName(row, "SQUAD_WORKING_PATTERN_NAME", "PATTERN ID"), "SquadWorkingPattern");

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, getColumnName(row, "NAME"), "Squad");
        }
    }

    /**
     * Validate Staff row
     */
    private void validateStaffRow(ExcelRowDTO row) {
        // Required fields
        validateRequiredField(row, "NAME", "Staff name is required");
        validateRequiredField(row, "SURNAME", "Staff surname is required");
        validateRequiredField(row, getColumnName(row, "REGISTRATION_CODE", "REGİSTRATİON CODE"), "Registration code is required");
        validateRequiredField(row, "DEPARTMENT ID", "Department is required");
        validateRequiredField(row, "SQUAD ID", "Squad is required");

        // String length validations
        validateStringLength(row, "NAME", 100, "Staff name");
        validateStringLength(row, "SURNAME", 100, "Staff surname");
        validateStringLength(row, getColumnName(row, "REGISTRATION_CODE", "REGİSTRATİON CODE"), 50, "Registration code");
        validateStringLength(row, "EMAIL", 100, "Email");
        validateStringLength(row, "PHONE", 20, "Phone");

        // Format validations
        validateRegistrationCodeFormat(row, getColumnName(row, "REGISTRATION_CODE", "REGİSTRATİON CODE"));
        validateEmailFormat(row, "EMAIL");
        validatePhoneFormat(row, "PHONE");

        // Foreign key validations
        validateForeignKey(row, "DEPARTMENT ID", "Department");
        validateForeignKey(row, "SQUAD ID", "Squad");
        validateManyToManyForeignKeys(row, getColumnName(row, "QUALİFİCATİONS", "QUALİFİCATİON IDS"), "Qualification");

        // Uniqueness validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByField(row, getColumnName(row, "REGISTRATION_CODE", "REGİSTRATİON CODE"), "Staff", "registration code");
            validateUniquenessByField(row, "EMAIL", "Staff", "email");
        }
    }

    /**
     * Validate Task row
     */
    private void validateTaskRow(ExcelRowDTO row) {
        // Required fields with column name variations
        validateRequiredField(row, getColumnName(row, "NAME"), "Task name is required");
        validateRequiredField(row, getColumnName(row, "START_TIME", "START TİME"), "Start time is required");
        validateRequiredField(row, getColumnName(row, "END_TIME", "END TİME"), "End time is required");
        validateRequiredField(row, getColumnName(row, "PRIORITY", "PRİORİTY"), "Priority is required");
        validateRequiredField(row, getColumnName(row, "DEPARTMENT_NAME", "DEPARTMENT ID"), "Department is required");

        // String length validations
        validateStringLength(row, getColumnName(row, "NAME"), 100, "Task name");
        validateStringLength(row, getColumnName(row, "DESCRIPTION", "DESCRİPTİON"), 500, "Task description");

        // DateTime format validation
        LocalDateTime startTime = validateDateTimeFormat(row, getColumnName(row, "START_TIME", "START TİME"));
        LocalDateTime endTime = validateDateTimeFormat(row, getColumnName(row, "END_TIME", "END TİME"));

        // DateTime logic validation
        if (startTime != null && endTime != null) {
            if (startTime.equals(endTime)) {
                row.addError(ExcelErrorDTO.error(getColumnName(row, "END_TIME", "END TİME"),
                        "Start time and end time cannot be the same"));
            } else if (startTime.isAfter(endTime)) {
                row.addError(ExcelErrorDTO.error(getColumnName(row, "END_TIME", "END TİME"),
                        "Start time cannot be after end time"));
            }
        }

        // Priority validation
        validateIntegerRange(row, getColumnName(row, "PRIORITY", "PRİORİTY"), 1, 10, "Priority");

        // Foreign key validations
        validateForeignKey(row, getColumnName(row, "DEPARTMENT_NAME", "DEPARTMENT ID"), "Department");
        validateManyToManyForeignKeys(row, getColumnName(row, "REQUIRED_QUALIFICATIONS", "REQUİRE QUALİFİCATİON IDS"), "Qualification");

        // Business logic validations
        if (row.getOperation() == EntityOperation.ADD) {
            validateUniquenessByName(row, getColumnName(row, "NAME"), "Task");
        }
    }

    /**
     * Validate DayOffRule row
     */
    private void validateDayOffRuleRow(ExcelRowDTO row) {
        // Required fields
        validateRequiredField(row, "WORKING_DAYS", "Working days is required");
        validateRequiredField(row, "OFF_DAYS", "Off days is required");
        validateRequiredField(row, "STAFF_REGISTRATION_CODE", "Staff registration code is required");

        // Range validations
        validateIntegerRange(row, "WORKING_DAYS", 1, 14, "Working days");
        validateIntegerRange(row, "OFF_DAYS", 1, 7, "Off days");

        // Fixed off days format validation (optional field)
        validateFixedOffDaysFormat(row, "FIXED_OFF_DAYS");

        // Foreign key validations
        validateStaffByRegistrationCode(row, "STAFF_REGISTRATION_CODE");
    }

    /**
     * Validate ConstraintOverride row
     */
    private void validateConstraintOverrideRow(ExcelRowDTO row) {
        // Required fields
        validateRequiredField(row, "OVERRIDE_VALUE", "Override value is required");
        validateRequiredField(row, "STAFF_REGISTRATION_CODE", "Staff registration code is required");
        validateRequiredField(row, "CONSTRAINT_NAME", "Constraint name is required");

        // String length validations
        validateStringLength(row, "OVERRIDE_VALUE", 100, "Override value");

        // Foreign key validations
        validateStaffByRegistrationCode(row, "STAFF_REGISTRATION_CODE");
        validateConstraintByName(row, "CONSTRAINT_NAME");
    }

    // ==================== HELPER VALIDATION METHODS ====================

    /**
     * Get column name from available options (handles column name variations)
     */
    private String getColumnName(ExcelRowDTO row, String... possibleNames) {
        if (row.getCellValues() == null) return possibleNames[0];

        for (String name : possibleNames) {
            if (row.getCellValues().containsKey(name.toUpperCase())) {
                return name.toUpperCase();
            }
        }

        // Return first option as fallback
        return possibleNames[0].toUpperCase();
    }

    /**
     * Validate required field
     */
    private void validateRequiredField(ExcelRowDTO row, String fieldName, String message) {
        if (!StringUtils.hasText(row.getCellValue(fieldName))) {
            row.addError(ExcelErrorDTO.requiredFieldError(fieldName));
        }
    }

    /**
     * Validate string length
     */
    private void validateStringLength(ExcelRowDTO row, String fieldName, int maxLength, String displayName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value) && value.length() > maxLength) {
            row.addError(ExcelErrorDTO.error(fieldName,
                    displayName + " cannot exceed " + maxLength + " characters"));
        }
    }

    /**
     * Validate time format
     */
    private LocalTime validateTimeFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (!StringUtils.hasText(value)) return null;

        try {
            return LocalTime.parse(value);
        } catch (DateTimeParseException e) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "HH:mm"));
            return null;
        }
    }

    /**
     * Validate date format
     */
    private LocalDate validateDateFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (!StringUtils.hasText(value)) return null;

        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "yyyy-MM-dd"));
            return null;
        }
    }

    /**
     * Validate datetime format
     */
    private LocalDateTime validateDateTimeFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (!StringUtils.hasText(value)) return null;

        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "yyyy-MM-ddTHH:mm:ss"));
            return null;
        }
    }

    /**
     * Validate integer range
     */
    private Integer validateIntegerRange(ExcelRowDTO row, String fieldName, int min, int max, String displayName) {
        String value = row.getCellValue(fieldName);
        if (!StringUtils.hasText(value)) return null;

        try {
            Integer intValue = Integer.parseInt(value);
            if (intValue < min || intValue > max) {
                row.addError(ExcelErrorDTO.error(fieldName,
                        displayName + " must be between " + min + " and " + max));
            }
            return intValue;
        } catch (NumberFormatException e) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "integer"));
            return null;
        }
    }

    /**
     * Validate boolean field
     */
    private void validateBooleanField(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            String normalized = value.toLowerCase().trim();
            if (!Arrays.asList("true", "false", "yes", "no", "1", "0").contains(normalized)) {
                row.addError(ExcelErrorDTO.formatError(fieldName, value, "true/false, yes/no, 1/0"));
            }
        }
    }

    /**
     * Validate email format
     */
    private void validateEmailFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value) && !EMAIL_PATTERN.matcher(value).matches()) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "valid email address"));
        }
    }

    /**
     * Validate phone format
     */
    private void validatePhoneFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value) && !PHONE_PATTERN.matcher(value).matches()) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value, "valid phone number"));
        }
    }

    /**
     * Validate registration code format
     */
    private void validateRegistrationCodeFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value) && !REGISTRATION_CODE_PATTERN.matcher(value).matches()) {
            row.addError(ExcelErrorDTO.formatError(fieldName, value,
                    "letters, numbers, underscore and dash only"));
        }
    }

    /**
     * Validate fixed off days format
     */
    private void validateFixedOffDaysFormat(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (!StringUtils.hasText(value)) return;

        String[] validDays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String[] days = value.split(",");

        for (String day : days) {
            String trimmedDay = day.trim().toUpperCase();
            if (!Arrays.asList(validDays).contains(trimmedDay)) {
                row.addError(ExcelErrorDTO.formatError(fieldName, value,
                        "comma-separated day names (MONDAY,TUESDAY,...)"));
                break;
            }
        }
    }

    /**
     * Validate foreign key reference
     */
    private void validateForeignKey(ExcelRowDTO row, String fieldName, String entityType) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            // Use foreign key resolver to validate
            try {
                Long resolvedId = null;
                switch (entityType.toLowerCase()) {
                    case "department":
                        resolvedId = foreignKeyResolver.resolveDepartmentId(value);
                        break;
                    case "qualification":
                        resolvedId = foreignKeyResolver.resolveQualificationId(value);
                        break;
                    case "workingperiod":
                        resolvedId = foreignKeyResolver.resolveWorkingPeriodId(value);
                        break;
                    case "squadworkingpattern":
                        resolvedId = foreignKeyResolver.resolveSquadWorkingPatternId(value);
                        break;
                    case "shift":
                        resolvedId = foreignKeyResolver.resolveShiftId(value);
                        break;
                    case "squad":
                        resolvedId = foreignKeyResolver.resolveSquadId(value);
                        break;
                    case "staff":
                        resolvedId = foreignKeyResolver.resolveStaffId(value);
                        break;
                    default:
                        foreignKeyResolver.validateEntityExists(entityType, value);
                }
                
                // If resolvedId is -1L, it means entity will be created in this batch - no error
                if (resolvedId != null && resolvedId == -1L) {
                    // Add info message that this entity will be created in batch
                    row.addError(ExcelErrorDTO.info(fieldName, 
                            entityType + " '" + value + "' will be created in this import batch"));
                }
                
            } catch (Exception e) {
                // Only add error if it's not a batch entity
                if (!e.getMessage().contains("will be created in this batch")) {
                    row.addError(ExcelErrorDTO.foreignKeyError(fieldName, entityType, value));
                }
            }
        }
    }

    /**
     * Validate many-to-many foreign keys
     */
    private void validateManyToManyForeignKeys(ExcelRowDTO row, String fieldName, String entityType) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            String[] values = value.split(",");
            for (String val : values) {
                String trimmedVal = val.trim();
                if (StringUtils.hasText(trimmedVal)) {
                    try {
                        Long resolvedId = null;
                        switch (entityType.toLowerCase()) {
                            case "qualification":
                                resolvedId = foreignKeyResolver.resolveQualificationId(trimmedVal);
                                break;
                            default:
                                foreignKeyResolver.validateEntityExists(entityType, trimmedVal);
                        }
                        
                        // If resolvedId is -1L, it means entity will be created in this batch - no error
                        if (resolvedId != null && resolvedId == -1L) {
                            // Add info message that this entity will be created in batch
                            row.addError(ExcelErrorDTO.info(fieldName, 
                                    entityType + " '" + trimmedVal + "' will be created in this import batch"));
                        }
                        
                    } catch (Exception e) {
                        // Only add error if it's not a batch entity
                        if (!e.getMessage().contains("will be created in this batch")) {
                            row.addError(ExcelErrorDTO.foreignKeyError(fieldName, entityType, trimmedVal));
                        }
                    }
                }
            }
        }
    }

    /**
     * Validate staff by registration code
     */
    private void validateStaffByRegistrationCode(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            try {
                foreignKeyResolver.validateStaffByRegistrationCode(value);
            } catch (Exception e) {
                row.addError(ExcelErrorDTO.foreignKeyError(fieldName, "Staff", value));
            }
        }
    }

    /**
     * Validate constraint by name
     */
    private void validateConstraintByName(ExcelRowDTO row, String fieldName) {
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            try {
                foreignKeyResolver.validateConstraintByName(value);
            } catch (Exception e) {
                row.addError(ExcelErrorDTO.foreignKeyError(fieldName, "Constraint", value));
            }
        }
    }

    /**
     * Validate uniqueness by name (placeholder - will be enhanced)
     */
    private void validateUniquenessByName(ExcelRowDTO row, String fieldName, String entityType) {
        // This is a placeholder - in real implementation, we'd check against database
        // and against other rows in the same upload batch
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            // TODO: Implement actual uniqueness check
        }
    }

    /**
     * Validate uniqueness by field (placeholder - will be enhanced)
     */
    private void validateUniquenessByField(ExcelRowDTO row, String fieldName, String entityType, String displayName) {
        // This is a placeholder
        String value = row.getCellValue(fieldName);
        if (StringUtils.hasText(value)) {
            // TODO: Implement actual uniqueness check
        }
    }
}
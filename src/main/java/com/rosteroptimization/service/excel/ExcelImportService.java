package com.rosteroptimization.service.excel;

import com.rosteroptimization.dto.*;
import com.rosteroptimization.service.*;
import com.rosteroptimization.service.excel.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for importing validated Excel data into database
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final DepartmentService departmentService;
    private final QualificationService qualificationService;
    private final WorkingPeriodService workingPeriodService;
    private final SquadWorkingPatternService squadWorkingPatternService;
    private final ShiftService shiftService;
    private final SquadService squadService;
    private final StaffService staffService;
    private final TaskService taskService;
    private final DayOffRuleService dayOffRuleService;
    private final ConstraintOverrideService constraintOverrideService;
    private final ExcelForeignKeyResolver foreignKeyResolver;

    /**
     * Import selected rows in dependency order
     */
    public ImportResultDTO importRows(List<ExcelRowDTO> selectedRows) {
        log.info("Starting import of {} selected rows", selectedRows.size());

        ImportResultDTO result = createImportResult();

        try {
            // Warm up caches and start batch validation
            foreignKeyResolver.clearCaches();
            foreignKeyResolver.warmUpCaches();
            foreignKeyResolver.startBatchValidation();
            
            // Collect batch entities first (same as validation does)
            for (ExcelRowDTO row : selectedRows) {
                EntityOperation operation = row.getOperation();
                if (operation == null) {
                    operation = row.getOperationFromCell();
                }
                if (operation == EntityOperation.ADD && row.getEntityType() != null) {
                    foreignKeyResolver.addToBatch(row.getEntityType(), row);
                }
            }

            // Group by entity type
            Map<ExcelEntityType, List<ExcelRowDTO>> groupedRows =
                    selectedRows.stream().collect(Collectors.groupingBy(ExcelRowDTO::getEntityType));

            // Clear batch context before import - entities will be in database now
            foreignKeyResolver.clearBatchContext();
            
            // Process in dependency order
            for (int order = 1; order <= ExcelEntityType.getMaxProcessingOrder(); order++) {
                ExcelEntityType[] entityTypes = ExcelEntityType.getByProcessingOrder(order);

                for (ExcelEntityType entityType : entityTypes) {
                    List<ExcelRowDTO> rows = groupedRows.get(entityType);
                    if (rows != null && !rows.isEmpty()) {
                        processEntityBatch(entityType, rows, result);
                    }
                }
            }

            result.setImportCompletedAt(LocalDateTime.now());
            result.updateStatistics();

        } catch (Exception e) {
            log.error("Critical error during import: {}", e.getMessage(), e);
            handleCriticalError(result, e);
        } finally {
            // Clean up batch context
            foreignKeyResolver.clearBatchContext();
        }

        return result;
    }

    /**
     * Process batch of rows for single entity type
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEntityBatch(ExcelEntityType entityType, List<ExcelRowDTO> rows, ImportResultDTO result) {
        log.info("Processing {} rows for {}", rows.size(), entityType.name());

        ImportResultDTO.EntityImportResult entityResult = createEntityResult(entityType, rows.size());

        for (ExcelRowDTO row : rows) {
            try {
                ImportResultDTO.ImportOperationDTO operation = processRowWithTransaction(entityType, row);
                result.addOperation(operation);

                if (operation.isSuccess()) {
                    entityResult.setSucceeded(entityResult.getSucceeded() + 1);
                    
                    // Real-time entity tracking: Successfully created entity'yi cache'e ekle
                    if (operation.getEntityId() != null && row.getOperation() == EntityOperation.ADD) {
                        updateBatchWithRealId(entityType, row, operation.getEntityId());
                    }
                } else {
                    entityResult.setFailed(entityResult.getFailed() + 1);
                    entityResult.getErrors().add(operation.getErrorMessage());
                }

            } catch (Exception e) {
                log.error("Error processing row {} ({}): {}", row.getRowId(), entityType, e.getMessage());
                log.debug("Full error details for row {}: ", row.getRowId(), e);
                handleRowError(row, entityType, e, result, entityResult);
            }
        }

        result.getEntityResults().put(entityType.name(), entityResult);
        log.info("Completed {}: {} succeeded, {} failed",
                entityType.name(), entityResult.getSucceeded(), entityResult.getFailed());
    }
    
    /**
     * Update batch context with real entity ID after successful import
     */
    private void updateBatchWithRealId(ExcelEntityType entityType, ExcelRowDTO row, Long realId) {
        // TODO: ForeignKeyResolver'a gerçek ID'yi bildirmek için
        // cache'i update etmek gerekebilir (gelecek geliştirme)
    }

    /**
     * Process single row with its own transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public ImportResultDTO.ImportOperationDTO processRowWithTransaction(ExcelEntityType entityType, ExcelRowDTO row) {
        return processRow(entityType, row);
    }

    /**
     * Process single row
     */
    public ImportResultDTO.ImportOperationDTO processRow(ExcelEntityType entityType, ExcelRowDTO row) {
        ImportResultDTO.ImportOperationDTO operation = createOperation(row, entityType);

        try {
            Long entityId = null;

            switch (entityType) {
                case DEPARTMENT:
                    entityId = processDepartment(row);
                    break;
                case QUALIFICATION:
                    entityId = processQualification(row);
                    break;
                case WORKING_PERIOD:
                    entityId = processWorkingPeriod(row);
                    break;
                case SQUAD_WORKING_PATTERN:
                    entityId = processSquadWorkingPattern(row);
                    break;
                case SHIFT:
                    entityId = processShift(row);
                    break;
                case SQUAD:
                    entityId = processSquad(row);
                    break;
                case STAFF:
                    entityId = processStaff(row);
                    break;
                case TASK:
                    entityId = processTask(row);
                    break;
                case DAY_OFF_RULE:
                    entityId = processDayOffRule(row);
                    break;
                case CONSTRAINT_OVERRIDE:
                    entityId = processConstraintOverride(row);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown entity type: " + entityType);
            }

            operation.setSuccess(true);
            operation.setEntityId(entityId);

        } catch (Exception e) {
            operation.setSuccess(false);
            String errorMessage = e.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = e.getClass().getSimpleName() + " occurred";
            }
            operation.setErrorMessage(errorMessage);
            log.warn("Row {} failed: {}", row.getRowId(), errorMessage);
        }

        return operation;
    }

    // ==================== ENTITY PROCESSORS ====================

    private Long processDepartment(ExcelRowDTO row) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setActive(true);

        return executeOperation(row, dto, departmentService);
    }

    private Long processQualification(ExcelRowDTO row) {
        QualificationDTO dto = new QualificationDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setActive(true);

        return executeOperation(row, dto, qualificationService);
    }

    private Long processWorkingPeriod(ExcelRowDTO row) {
        WorkingPeriodDTO dto = new WorkingPeriodDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setStartTime(LocalTime.parse(getCellValue(row, "START_TIME", "START TİME")));
        dto.setEndTime(LocalTime.parse(getCellValue(row, "END_TIME", "END TİME")));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setActive(true);

        return executeOperation(row, dto, workingPeriodService);
    }

    private Long processSquadWorkingPattern(ExcelRowDTO row) {
        SquadWorkingPatternDTO dto = new SquadWorkingPatternDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setShiftPattern(getCellValue(row, "SHIFT_PATTERN", "PATTERN"));
        dto.setCycleLength(Integer.parseInt(getCellValue(row, "CYCLE_LENGTH", "CYCLE LENGTH")));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setActive(true);

        return executeOperation(row, dto, squadWorkingPatternService);
    }

    private Long processShift(ExcelRowDTO row) {
        ShiftDTO dto = new ShiftDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setStartTime(LocalTime.parse(getCellValue(row, "START_TIME", "START TİME")));
        dto.setEndTime(LocalTime.parse(getCellValue(row, "END_TIME", "END TİME")));
        dto.setIsNightShift(parseBoolean(getCellValue(row, "IS_NIGHT_SHIFT")));
        dto.setFixed(parseBoolean(getCellValue(row, "FIXED", "FİXED")));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setWorkingPeriodId(foreignKeyResolver.resolveWorkingPeriodId(
                getCellValue(row, "WORKING_PERIOD_NAME", "WORKİNG PERİOD ID")));
        dto.setActive(true);

        return executeOperation(row, dto, shiftService);
    }

    private Long processSquad(ExcelRowDTO row) {
        SquadDTO dto = new SquadDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setStartDate(LocalDate.parse(getCellValue(row, "START_DATE", "START DATE")));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        dto.setSquadWorkingPatternId(foreignKeyResolver.resolveSquadWorkingPatternId(
                getCellValue(row, "SQUAD_WORKING_PATTERN_NAME", "PATTERN ID")));
        dto.setActive(true);

        return executeOperation(row, dto, squadService);
    }

    private Long processStaff(ExcelRowDTO row) {
        StaffDTO dto = new StaffDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setSurname(getCellValue(row, "SURNAME"));
        dto.setRegistrationCode(getCellValue(row, "REGISTRATION_CODE", "REGİSTRATİON CODE"));
        dto.setEmail(getCellValue(row, "EMAIL", "EMAİL"));
        dto.setPhone(getCellValue(row, "PHONE"));
        Long departmentId = foreignKeyResolver.resolveDepartmentId(
                getCellValue(row, "DEPARTMENT ID"));
        if (departmentId != null && departmentId == -1L) {
            throw new IllegalArgumentException("Department reference not resolved: " + getCellValue(row, "DEPARTMENT ID"));
        }
        dto.setDepartmentId(departmentId);
        Long squadId = foreignKeyResolver.resolveSquadId(
                getCellValue(row, "SQUAD ID"));
        if (squadId != null && squadId == -1L) {
            throw new IllegalArgumentException("Squad reference not resolved: " + getCellValue(row, "SQUAD ID"));
        }
        dto.setSquadId(squadId);
        dto.setQualificationIds(new HashSet<>(foreignKeyResolver.resolveQualificationIds(
                getCellValue(row, "QUALİFİCATİON IDS", "QUALIFICATIONS"))));
        dto.setActive(true);

        return executeOperation(row, dto, staffService);
    }

    private Long processTask(ExcelRowDTO row) {
        TaskDTO dto = new TaskDTO();
        dto.setName(getCellValue(row, "NAME"));
        dto.setStartTime(LocalDateTime.parse(getCellValue(row, "START_TIME", "START TİME")));
        dto.setEndTime(LocalDateTime.parse(getCellValue(row, "END_TIME", "END TİME")));
        dto.setPriority(Integer.parseInt(getCellValue(row, "PRIORITY", "PRİORİTY")));
        dto.setDescription(getCellValue(row, "DESCRIPTION", "DESCRİPTİON"));
        Long departmentId = foreignKeyResolver.resolveDepartmentId(
                getCellValue(row, "DEPARTMENT_NAME", "DEPARTMENT ID"));
        if (departmentId != null && departmentId == -1L) {
            throw new IllegalArgumentException("Department reference not resolved: " + getCellValue(row, "DEPARTMENT_NAME", "DEPARTMENT ID"));
        }
        dto.setDepartmentId(departmentId);
        dto.setRequiredQualificationIds(new HashSet<>(foreignKeyResolver.resolveQualificationIds(
                getCellValue(row, "REQUIRED_QUALIFICATIONS", "REQUİRE QUALİFİCATİON IDS"))));
        dto.setActive(true);

        return executeOperation(row, dto, taskService);
    }

    private Long processDayOffRule(ExcelRowDTO row) {
        DayOffRuleDTO dto = new DayOffRuleDTO();
        dto.setWorkingDays(Integer.parseInt(getCellValue(row, "WORKING_DAYS")));
        dto.setOffDays(Integer.parseInt(getCellValue(row, "OFF_DAYS")));
        dto.setFixedOffDays(getCellValue(row, "FIXED_OFF_DAYS"));
        dto.setStaffId(foreignKeyResolver.resolveStaffIdByRegistrationCode(
                getCellValue(row, "STAFF_REGISTRATION_CODE")));

        return executeOperation(row, dto, dayOffRuleService);
    }

    private Long processConstraintOverride(ExcelRowDTO row) {
        ConstraintOverrideDTO dto = new ConstraintOverrideDTO();
        dto.setOverrideValue(getCellValue(row, "OVERRIDE_VALUE"));
        dto.setStaffId(foreignKeyResolver.resolveStaffIdByRegistrationCode(
                getCellValue(row, "STAFF_REGISTRATION_CODE")));
        dto.setConstraintId(foreignKeyResolver.resolveConstraintIdByName(
                getCellValue(row, "CONSTRAINT_NAME")));

        return executeOperation(row, dto, constraintOverrideService);
    }

    // ==================== HELPER METHODS ====================

    private String getCellValue(ExcelRowDTO row, String... possibleNames) {
        if (row.getCellValues() == null) return "";

        for (String name : possibleNames) {
            String value = row.getCellValues().get(name.toUpperCase());
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private Boolean parseBoolean(String value) {
        if (!StringUtils.hasText(value)) return null;

        String normalized = value.toLowerCase().trim();
        switch (normalized) {
            case "true":
            case "yes":
            case "1":
                return true;
            case "false":
            case "no":
            case "0":
                return false;
            default:
                return null;
        }
    }

    private <T> Long executeOperation(ExcelRowDTO row, T dto, Object service) {
        switch (row.getOperation()) {
            case ADD:
                if (service instanceof DepartmentService) {
                    DepartmentDTO created = ((DepartmentService) service).create((DepartmentDTO) dto);
                    return created.getId();
                } else if (service instanceof QualificationService) {
                    QualificationDTO created = ((QualificationService) service).create((QualificationDTO) dto);
                    return created.getId();
                } else if (service instanceof WorkingPeriodService) {
                    WorkingPeriodDTO created = ((WorkingPeriodService) service).create((WorkingPeriodDTO) dto);
                    return created.getId();
                } else if (service instanceof SquadWorkingPatternService) {
                    SquadWorkingPatternDTO created = ((SquadWorkingPatternService) service).create((SquadWorkingPatternDTO) dto);
                    return created.getId();
                } else if (service instanceof ShiftService) {
                    ShiftDTO created = ((ShiftService) service).create((ShiftDTO) dto);
                    return created.getId();
                } else if (service instanceof SquadService) {
                    SquadDTO created = ((SquadService) service).create((SquadDTO) dto);
                    return created.getId();
                } else if (service instanceof StaffService) {
                    StaffDTO created = ((StaffService) service).create((StaffDTO) dto);
                    return created.getId();
                } else if (service instanceof TaskService) {
                    TaskDTO created = ((TaskService) service).create((TaskDTO) dto);
                    return created.getId();
                } else if (service instanceof DayOffRuleService) {
                    DayOffRuleDTO created = ((DayOffRuleService) service).create((DayOffRuleDTO) dto);
                    return created.getId();
                } else if (service instanceof ConstraintOverrideService) {
                    ConstraintOverrideDTO created = ((ConstraintOverrideService) service).create((ConstraintOverrideDTO) dto);
                    return created.getId();
                }
                break;

            case UPDATE:
                Long id = resolveEntityIdForUpdate(row);
                if (service instanceof DepartmentService) {
                    DepartmentDTO updated = ((DepartmentService) service).update(id, (DepartmentDTO) dto);
                    return updated.getId();
                } else if (service instanceof QualificationService) {
                    QualificationDTO updated = ((QualificationService) service).update(id, (QualificationDTO) dto);
                    return updated.getId();
                } else if (service instanceof WorkingPeriodService) {
                    WorkingPeriodDTO updated = ((WorkingPeriodService) service).update(id, (WorkingPeriodDTO) dto);
                    return updated.getId();
                } else if (service instanceof SquadWorkingPatternService) {
                    SquadWorkingPatternDTO updated = ((SquadWorkingPatternService) service).update(id, (SquadWorkingPatternDTO) dto);
                    return updated.getId();
                } else if (service instanceof ShiftService) {
                    ShiftDTO updated = ((ShiftService) service).update(id, (ShiftDTO) dto);
                    return updated.getId();
                } else if (service instanceof SquadService) {
                    SquadDTO updated = ((SquadService) service).update(id, (SquadDTO) dto);
                    return updated.getId();
                } else if (service instanceof StaffService) {
                    StaffDTO updated = ((StaffService) service).update(id, (StaffDTO) dto);
                    return updated.getId();
                } else if (service instanceof TaskService) {
                    TaskDTO updated = ((TaskService) service).update(id, (TaskDTO) dto);
                    return updated.getId();
                } else if (service instanceof DayOffRuleService) {
                    DayOffRuleDTO updated = ((DayOffRuleService) service).update(id, (DayOffRuleDTO) dto);
                    return updated.getId();
                } else if (service instanceof ConstraintOverrideService) {
                    ConstraintOverrideDTO updated = ((ConstraintOverrideService) service).update(id, (ConstraintOverrideDTO) dto);
                    return updated.getId();
                }
                break;

            case DELETE:
                Long deleteId = resolveEntityIdForUpdate(row);
                if (service instanceof DepartmentService) {
                    ((DepartmentService) service).delete(deleteId);
                } else if (service instanceof QualificationService) {
                    ((QualificationService) service).delete(deleteId);
                } else if (service instanceof WorkingPeriodService) {
                    ((WorkingPeriodService) service).delete(deleteId);
                } else if (service instanceof SquadWorkingPatternService) {
                    ((SquadWorkingPatternService) service).delete(deleteId);
                } else if (service instanceof ShiftService) {
                    ((ShiftService) service).delete(deleteId);
                } else if (service instanceof SquadService) {
                    ((SquadService) service).delete(deleteId);
                } else if (service instanceof StaffService) {
                    ((StaffService) service).delete(deleteId);
                } else if (service instanceof TaskService) {
                    ((TaskService) service).delete(deleteId);
                } else if (service instanceof DayOffRuleService) {
                    ((DayOffRuleService) service).delete(deleteId);
                } else if (service instanceof ConstraintOverrideService) {
                    ((ConstraintOverrideService) service).delete(deleteId);
                }
                return deleteId;
        }

        throw new IllegalArgumentException("Unknown operation or service type");
    }

    private Long resolveEntityIdForUpdate(ExcelRowDTO row) {
        ParsedEntityId parsedId = row.parseMainEntityId();
        if (parsedId.hasId()) {
            return parsedId.getId();
        } else if (parsedId.hasName()) {
            // Try to resolve by name - this is simplified, real implementation would be more specific
            throw new UnsupportedOperationException("Name-based UPDATE not implemented yet");
        } else {
            throw new IllegalArgumentException("UPDATE/DELETE requires entity ID or name");
        }
    }

    private ImportResultDTO createImportResult() {
        ImportResultDTO result = new ImportResultDTO();
        result.setImportStartedAt(LocalDateTime.now());
        result.setOperations(new ArrayList<>());
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        result.setInfoMessages(new ArrayList<>());
        result.setEntityResults(new HashMap<>());
        return result;
    }

    private ImportResultDTO.EntityImportResult createEntityResult(ExcelEntityType entityType, int totalRows) {
        ImportResultDTO.EntityImportResult result = new ImportResultDTO.EntityImportResult();
        result.setEntityType(entityType.name());
        result.setProcessed(totalRows);
        result.setSucceeded(0);
        result.setFailed(0);
        result.setSkipped(0);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());
        return result;
    }

    private ImportResultDTO.ImportOperationDTO createOperation(ExcelRowDTO row, ExcelEntityType entityType) {
        ImportResultDTO.ImportOperationDTO operation = new ImportResultDTO.ImportOperationDTO();
        operation.setRowId(row.getRowId());
        operation.setEntityType(entityType.name());
        operation.setOperation(row.getOperation());
        operation.setProcessedAt(LocalDateTime.now());
        return operation;
    }

    private void handleCriticalError(ImportResultDTO result, Exception e) {
        ImportResultDTO.ImportErrorDTO error = new ImportResultDTO.ImportErrorDTO();
        error.setErrorMessage("Critical import error: " + e.getMessage());
        error.setSeverity(ErrorSeverity.ERROR);
        error.setOccurredAt(LocalDateTime.now());
        result.addError(error);

        result.setImportCompletedAt(LocalDateTime.now());
        result.updateStatistics();
    }

    private void handleRowError(ExcelRowDTO row, ExcelEntityType entityType, Exception e,
                                ImportResultDTO result, ImportResultDTO.EntityImportResult entityResult) {
        ImportResultDTO.ImportOperationDTO operation = new ImportResultDTO.ImportOperationDTO();
        operation.setRowId(row.getRowId());
        operation.setEntityType(entityType.name());
        operation.setOperation(row.getOperation());
        operation.setSuccess(false);
        operation.setErrorMessage("Processing error: " + e.getMessage());
        operation.setProcessedAt(LocalDateTime.now());

        result.addOperation(operation);
        entityResult.setFailed(entityResult.getFailed() + 1);
        entityResult.getErrors().add(e.getMessage());
    }
}
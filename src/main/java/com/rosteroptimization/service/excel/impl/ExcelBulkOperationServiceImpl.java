package com.rosteroptimization.service.excel.impl;

import com.rosteroptimization.dto.*;
import com.rosteroptimization.dto.excel.ExcelRowPreview;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.dto.excel.ImportResult;
import com.rosteroptimization.enums.ExcelOperation;
import com.rosteroptimization.service.*;
import com.rosteroptimization.service.excel.ExcelBulkOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExcelBulkOperationServiceImpl implements ExcelBulkOperationService {

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

    @Override
    public ImportResult applyDepartmentOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} department operations", results.size());
        return applyOperationsWithTransaction(results, "Department", (dto, operation) -> {
            DepartmentDTO departmentDTO = (DepartmentDTO) dto;
            switch (operation) {
                case ADD:
                    return departmentService.create(departmentDTO);
                case UPDATE:
                    return departmentService.update(departmentDTO.getId(), departmentDTO);
                case DELETE:
                    departmentService.delete(departmentDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyQualificationOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} qualification operations", results.size());
        return applyOperations(results, "Qualification", (dto, operation) -> {
            QualificationDTO qualificationDTO = (QualificationDTO) dto;
            switch (operation) {
                case ADD:
                    return qualificationService.create(qualificationDTO);
                case UPDATE:
                    return qualificationService.update(qualificationDTO.getId(), qualificationDTO);
                case DELETE:
                    qualificationService.delete(qualificationDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyWorkingPeriodOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} working period operations", results.size());
        return applyOperations(results, "WorkingPeriod", (dto, operation) -> {
            WorkingPeriodDTO workingPeriodDTO = (WorkingPeriodDTO) dto;
            switch (operation) {
                case ADD:
                    return workingPeriodService.create(workingPeriodDTO);
                case UPDATE:
                    return workingPeriodService.update(workingPeriodDTO.getId(), workingPeriodDTO);
                case DELETE:
                    workingPeriodService.delete(workingPeriodDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applySquadWorkingPatternOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} squad working pattern operations", results.size());
        return applyOperations(results, "SquadWorkingPattern", (dto, operation) -> {
            SquadWorkingPatternDTO squadWorkingPatternDTO = (SquadWorkingPatternDTO) dto;
            switch (operation) {
                case ADD:
                    return squadWorkingPatternService.create(squadWorkingPatternDTO);
                case UPDATE:
                    return squadWorkingPatternService.update(squadWorkingPatternDTO.getId(), squadWorkingPatternDTO);
                case DELETE:
                    squadWorkingPatternService.delete(squadWorkingPatternDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyShiftOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} shift operations", results.size());
        return applyOperations(results, "Shift", (dto, operation) -> {
            ShiftDTO shiftDTO = (ShiftDTO) dto;
            switch (operation) {
                case ADD:
                    return shiftService.create(shiftDTO);
                case UPDATE:
                    return shiftService.update(shiftDTO.getId(), shiftDTO);
                case DELETE:
                    shiftService.delete(shiftDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applySquadOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} squad operations", results.size());
        return applyOperations(results, "Squad", (dto, operation) -> {
            SquadDTO squadDTO = (SquadDTO) dto;
            switch (operation) {
                case ADD:
                    return squadService.create(squadDTO);
                case UPDATE:
                    return squadService.update(squadDTO.getId(), squadDTO);
                case DELETE:
                    squadService.delete(squadDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyStaffOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} staff operations", results.size());
        return applyOperations(results, "Staff", (dto, operation) -> {
            StaffDTO staffDTO = (StaffDTO) dto;
            switch (operation) {
                case ADD:
                    return staffService.create(staffDTO);
                case UPDATE:
                    return staffService.update(staffDTO.getId(), staffDTO);
                case DELETE:
                    staffService.delete(staffDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyTaskOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} task operations", results.size());
        return applyOperations(results, "Task", (dto, operation) -> {
            TaskDTO taskDTO = (TaskDTO) dto;
            switch (operation) {
                case ADD:
                    return taskService.create(taskDTO);
                case UPDATE:
                    return taskService.update(taskDTO.getId(), taskDTO);
                case DELETE:
                    taskService.delete(taskDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyDayOffRuleOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} day off rule operations", results.size());
        return applyOperations(results, "DayOffRule", (dto, operation) -> {
            DayOffRuleDTO dayOffRuleDTO = (DayOffRuleDTO) dto;
            switch (operation) {
                case ADD:
                    return dayOffRuleService.create(dayOffRuleDTO);
                case UPDATE:
                    return dayOffRuleService.update(dayOffRuleDTO.getId(), dayOffRuleDTO);
                case DELETE:
                    dayOffRuleService.delete(dayOffRuleDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyConstraintOverrideOperations(List<ExcelRowResult<?>> results) {
        log.info("Applying {} constraint override operations", results.size());
        return applyOperations(results, "ConstraintOverride", (dto, operation) -> {
            ConstraintOverrideDTO constraintOverrideDTO = (ConstraintOverrideDTO) dto;
            switch (operation) {
                case ADD:
                    return constraintOverrideService.create(constraintOverrideDTO);
                case UPDATE:
                    return constraintOverrideService.update(constraintOverrideDTO.getId(), constraintOverrideDTO);
                case DELETE:
                    constraintOverrideService.delete(constraintOverrideDTO.getId());
                    return null;
            }
            return null;
        });
    }

    @Override
    public ImportResult applyOperationsByEntityType(String entityType, List<ExcelRowResult<?>> results) {
        switch (entityType.toLowerCase()) {
            case "department":
            case "departments":
                return applyDepartmentOperations(results);
            case "qualification":
            case "qualifications":
                return applyQualificationOperations(results);
            case "workingperiod":
            case "workingperiods":
            case "working-period":
            case "working-periods":
                return applyWorkingPeriodOperations(results);
            case "squadworkingpattern":
            case "squadworkingpatterns":
            case "squad-working-pattern":
            case "squad-working-patterns":
                return applySquadWorkingPatternOperations(results);
            case "shift":
            case "shifts":
                return applyShiftOperations(results);
            case "squad":
            case "squads":
                return applySquadOperations(results);
            case "staff":
            case "staffs":
                return applyStaffOperations(results);
            case "task":
            case "tasks":
                return applyTaskOperations(results);
            case "dayoffrule":
            case "dayoffrules":
            case "day-off-rule":
            case "day-off-rules":
                return applyDayOffRuleOperations(results);
            case "constraintoverride":
            case "constraintoverrides":
            case "constraint-override":
            case "constraint-overrides":
                return applyConstraintOverrideOperations(results);
            default:
                ImportResult result = new ImportResult();
                result.setSuccess(false);
                result.setMessage("Unsupported entity type: " + entityType);
                return result;
        }
    }

    /**
     * Generic method to apply operations with individual transaction for each row
     */
    private ImportResult applyOperationsWithTransaction(List<ExcelRowResult<?>> results, String entityName,
                                                       ServiceOperation serviceOperation) {
        ImportResult result = new ImportResult();
        Map<String, Integer> appliedCounts = new HashMap<>();
        int successful = 0;
        int failed = 0;

        for (ExcelRowResult<?> rowResult : results) {
            if (!rowResult.isValid()) {
                failed++;
                continue;
            }

            try {
                Object dto = rowResult.getData();
                ExcelOperation operation = rowResult.getOperation();

                // Execute each operation in its own transaction to avoid rollback-only issues
                serviceOperation.execute(dto, operation);

                successful++;
                appliedCounts.put(operation.name(), appliedCounts.getOrDefault(operation.name(), 0) + 1);

            } catch (Exception e) {
                log.error("Error applying {} operation for row {}: {}", entityName, rowResult.getRowNumber(), e.getMessage());
                log.debug("Full error details: ", e);
                failed++;
                // Continue with other rows even if one fails
            }
        }

        result.setSuccess(failed == 0);
        result.setTotalProcessed(results.size());
        result.setSuccessfulOperations(successful);
        result.setFailedOperations(failed);
        result.setAppliedCounts(appliedCounts);
        
        if (failed > 0) {
            result.setMessage(String.format("Processed %d %s operations. Success: %d, Failed: %d. Some operations failed but successful ones were applied.",
                    results.size(), entityName, successful, failed));
        } else {
            result.setMessage(String.format("Successfully processed %d %s operations.",
                    results.size(), entityName));
        }

        return result;
    }

    /**
     * Legacy method - kept for backward compatibility
     */
    private ImportResult applyOperations(List<ExcelRowResult<?>> results, String entityName,
                                         ServiceOperation serviceOperation) {
        return applyOperationsWithTransaction(results, entityName, serviceOperation);
    }

    @Override
    public ImportResult applySelectedRows(List<ExcelRowPreview<?>> selectedRows, String entityType) {
        log.info("Applying {} selected rows for entity type: {}", selectedRows.size(), entityType);
        
        // Convert ExcelRowPreview to ExcelRowResult for compatibility
        @SuppressWarnings("unchecked")
        List<ExcelRowResult<?>> rowResults = (List<ExcelRowResult<?>>) (List<?>) selectedRows;
        
        // Apply via existing entity type handler
        ImportResult result = applyOperationsByEntityType(entityType, rowResults);
        
        // Update row statuses based on result
        updateRowStatuses(selectedRows, result);
        
        return result;
    }
    
    private void updateRowStatuses(List<ExcelRowPreview<?>> rows, ImportResult result) {
        if (result.isSuccess()) {
            // Mark all selected rows as imported if overall success
            rows.forEach(ExcelRowPreview::markAsImported);
        } else {
            // Mark rows as failed with general error message
            rows.forEach(row -> row.markAsFailed(result.getMessage()));
        }
    }

    /**
     * Functional interface for service operations
     */
    @FunctionalInterface
    private interface ServiceOperation {
        Object execute(Object dto, ExcelOperation operation) throws Exception;
    }
}
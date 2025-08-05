package com.rosteroptimization.service.excel;

import com.rosteroptimization.dto.excel.ExcelRowPreview;
import com.rosteroptimization.dto.excel.ExcelRowResult;
import com.rosteroptimization.dto.excel.ImportResult;
import java.util.List;

public interface ExcelBulkOperationService {

    /**
     * Apply bulk operations for Department
     */
    ImportResult applyDepartmentOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for Qualification
     */
    ImportResult applyQualificationOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for WorkingPeriod
     */
    ImportResult applyWorkingPeriodOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for SquadWorkingPattern
     */
    ImportResult applySquadWorkingPatternOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for Shift
     */
    ImportResult applyShiftOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for Squad
     */
    ImportResult applySquadOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for Staff
     */
    ImportResult applyStaffOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for Task
     */
    ImportResult applyTaskOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for DayOffRule
     */
    ImportResult applyDayOffRuleOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply bulk operations for ConstraintOverride
     */
    ImportResult applyConstraintOverrideOperations(List<ExcelRowResult<?>> results);

    /**
     * Apply operations based on entity type
     */
    ImportResult applyOperationsByEntityType(String entityType, List<ExcelRowResult<?>> results);
    
    /**
     * Apply selected rows from enhanced preview
     */
    ImportResult applySelectedRows(List<ExcelRowPreview<?>> selectedRows, String entityType);
}
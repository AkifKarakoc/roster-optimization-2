package com.rosteroptimization.service.excel;

import com.rosteroptimization.dto.*;
import com.rosteroptimization.entity.*;
import lombok.Getter;

/**
 * Enum defining Excel entity types and their processing order
 */
@Getter
public enum ExcelEntityType {

    // BATCH 1 - No dependencies
    DEPARTMENT("Department", Department.class, DepartmentDTO.class, 1),
    QUALIFICATION("Qualification", Qualification.class, QualificationDTO.class, 1),
    WORKING_PERIOD("WorkingPeriod", WorkingPeriod.class, WorkingPeriodDTO.class, 1),
    SQUAD_WORKING_PATTERN("SquadWorkingPattern", SquadWorkingPattern.class, SquadWorkingPatternDTO.class, 1),

    // BATCH 2 - Level 1 dependencies
    SHIFT("Shift", Shift.class, ShiftDTO.class, 2),
    SQUAD("Squad", Squad.class, SquadDTO.class, 2),

    // BATCH 3 - Level 2 dependencies
    STAFF("Staff", Staff.class, StaffDTO.class, 3),
    TASK("Task", Task.class, TaskDTO.class, 3),

    // BATCH 4 - Level 3 dependencies
    DAY_OFF_RULE("DayOffRule", DayOffRule.class, DayOffRuleDTO.class, 4),
    CONSTRAINT_OVERRIDE("ConstraintOverride", ConstraintOverride.class, ConstraintOverrideDTO.class, 4);

    private final String sheetName;
    private final Class<?> entityClass;
    private final Class<?> dtoClass;
    private final int processingOrder;

    ExcelEntityType(String sheetName, Class<?> entityClass, Class<?> dtoClass, int processingOrder) {
        this.sheetName = sheetName;
        this.entityClass = entityClass;
        this.dtoClass = dtoClass;
        this.processingOrder = processingOrder;
    }

    /**
     * Get entity types by processing order (for dependency-based import)
     */
    public static ExcelEntityType[] getByProcessingOrder(int order) {
        return java.util.Arrays.stream(values())
                .filter(type -> type.processingOrder == order)
                .toArray(ExcelEntityType[]::new);
    }

    /**
     * Get maximum processing order
     */
    public static int getMaxProcessingOrder() {
        return java.util.Arrays.stream(values())
                .mapToInt(ExcelEntityType::getProcessingOrder)
                .max()
                .orElse(1);
    }

    /**
     * Get entity type by sheet name (case-insensitive)
     */
    public static ExcelEntityType fromSheetName(String sheetName) {
        if (sheetName == null) return null;

        String normalizedName = sheetName.toLowerCase().trim();

        for (ExcelEntityType type : values()) {
            if (type.sheetName.toLowerCase().equals(normalizedName)) {
                return type;
            }
        }
        return null;
    }
}
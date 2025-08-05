package com.rosteroptimization.service.excel.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EntityParserFactory {

    // Simple parsers (no FK dependencies)
    private final DepartmentExcelParser departmentParser;
    private final QualificationExcelParser qualificationParser;
    private final WorkingPeriodExcelParser workingPeriodParser;

    // Complex parsers (with FK dependencies)
    private final SquadWorkingPatternExcelParser squadWorkingPatternParser;
    private final ShiftExcelParser shiftParser;
    private final SquadExcelParser squadParser;
    private final StaffExcelParser staffParser;
    private final TaskExcelParser taskParser;
    private final DayOffRuleExcelParser dayOffRuleParser;
    private final ConstraintOverrideExcelParser constraintOverrideParser;

    private final Map<String, EntityExcelParser<?>> parsers = new HashMap<>();

    public EntityExcelParser<?> getParser(String entityType) {
        if (parsers.isEmpty()) {
            initializeParsers();
        }

        return parsers.get(entityType.toLowerCase());
    }

    private void initializeParsers() {
        // Simple entities (no FK dependencies) - hem tekil hem çoğul formları
        parsers.put("department", departmentParser);
        parsers.put("departments", departmentParser);
        parsers.put("qualification", qualificationParser);
        parsers.put("qualifications", qualificationParser);
        parsers.put("workingperiod", workingPeriodParser);
        parsers.put("workingperiods", workingPeriodParser);
        parsers.put("working-period", workingPeriodParser);
        parsers.put("working-periods", workingPeriodParser);

        // Complex entities (with FK dependencies)
        parsers.put("squadworkingpattern", squadWorkingPatternParser);
        parsers.put("squadworkingpatterns", squadWorkingPatternParser);
        parsers.put("squad-working-pattern", squadWorkingPatternParser);
        parsers.put("squad-working-patterns", squadWorkingPatternParser);
        parsers.put("shift", shiftParser);
        parsers.put("shifts", shiftParser);
        parsers.put("squad", squadParser);
        parsers.put("squads", squadParser);
        parsers.put("staff", staffParser);
        parsers.put("staffs", staffParser);
        parsers.put("task", taskParser);
        parsers.put("tasks", taskParser);
        parsers.put("dayoffrule", dayOffRuleParser);
        parsers.put("dayoffrules", dayOffRuleParser);
        parsers.put("day-off-rule", dayOffRuleParser);
        parsers.put("day-off-rules", dayOffRuleParser);
        parsers.put("constraintoverride", constraintOverrideParser);
        parsers.put("constraintoverrides", constraintOverrideParser);
        parsers.put("constraint-override", constraintOverrideParser);
        parsers.put("constraint-overrides", constraintOverrideParser);
    }

    /**
     * Get all supported entity types
     */
    public String[] getSupportedEntityTypes() {
        if (parsers.isEmpty()) {
            initializeParsers();
        }
        return parsers.keySet().toArray(new String[0]);
    }

    /**
     * Check if entity type is supported
     */
    public boolean isSupported(String entityType) {
        if (parsers.isEmpty()) {
            initializeParsers();
        }
        return parsers.containsKey(entityType.toLowerCase());
    }
}
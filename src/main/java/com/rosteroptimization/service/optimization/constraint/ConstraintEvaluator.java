package com.rosteroptimization.service.optimization.constraint;

import com.rosteroptimization.entity.*;
import com.rosteroptimization.service.ConstraintService;
import com.rosteroptimization.service.optimization.model.Assignment;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Enhanced constraint evaluator with performance optimizations and better maintainability
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConstraintEvaluator {

    private final ConstraintService constraintService;

    // Performance cache
    private final Map<String, String> constraintCache = new ConcurrentHashMap<>();
    private volatile boolean cacheInitialized = false;

    /**
     * Main evaluation method with early termination support
     */
    public ConstraintEvaluationResult evaluateRosterPlan(RosterPlan rosterPlan) {
        return evaluateRosterPlan(rosterPlan, false);
    }

    /**
     * Evaluation with early termination option for performance
     */
    public ConstraintEvaluationResult evaluateRosterPlan(RosterPlan rosterPlan, boolean earlyTermination) {
        log.debug("Evaluating constraints for roster plan: {}", rosterPlan.getPlanId());

        ConstraintEvaluationResult result = new ConstraintEvaluationResult();
        initializeConstraintCacheIfNeeded();

        // Group assignments by staff for efficient processing
        Map<Long, List<Assignment>> staffAssignments = rosterPlan.getAssignments().stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getStaff().getId()));

        // Evaluate staff constraints with early termination
        for (Map.Entry<Long, List<Assignment>> entry : staffAssignments.entrySet()) {
            List<Assignment> assignments = entry.getValue();
            Staff staff = assignments.get(0).getStaff();

            evaluateStaffConstraints(staff, assignments, result);

            // Early termination if too many hard violations
            if (earlyTermination && result.getHardViolationCount() > 50) {
                log.debug("Early termination triggered at {} hard violations", result.getHardViolationCount());
                return result;
            }
        }

        // Global constraints evaluation
        evaluateGlobalConstraints(rosterPlan, result);

        log.debug("Constraint evaluation completed. Hard: {}, Soft: {}",
                result.getHardViolationCount(), result.getSoftViolationCount());

        return result;
    }

    /**
     * Staff-level constraint evaluation
     */
    private void evaluateStaffConstraints(Staff staff, List<Assignment> assignments,
                                          ConstraintEvaluationResult result) {

        Map<String, String> effectiveConstraints = getEffectiveConstraints(staff);

        // Core constraint checks
        checkWorkingHourLimits(staff, assignments, effectiveConstraints, result);
        checkRestPeriods(staff, assignments, effectiveConstraints, result);
        checkWorkingPatterns(staff, assignments, effectiveConstraints, result);
        checkQualificationMatch(staff, assignments, result);
        checkDayOffRules(staff, assignments, result);
    }

    /**
     * Working hours constraints (daily, weekly, monthly)
     */
    private void checkWorkingHourLimits(Staff staff, List<Assignment> assignments,
                                        Map<String, String> constraints, ConstraintEvaluationResult result) {

        double maxDaily = parseDoubleConstraint(constraints, "MaxWorkingHoursPerDay", 12.0);
        double maxWeekly = parseDoubleConstraint(constraints, "MaxWorkingHoursPerWeek", 40.0);
        double maxMonthly = parseDoubleConstraint(constraints, "MaxWorkingHoursPerMonth", 160.0);

        // Daily hours check
        Map<LocalDate, Double> dailyHours = assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getDate,
                        Collectors.summingDouble(Assignment::getDurationHours)));

        dailyHours.entrySet().stream()
                .filter(entry -> entry.getValue() > maxDaily)
                .forEach(entry -> result.addHardViolation(
                        String.format("Staff %s exceeds max daily hours (%.1f > %.1f) on %s",
                                getStaffName(staff), entry.getValue(), maxDaily, entry.getKey())));

        // Weekly hours check
        Map<Integer, Double> weeklyHours = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().get(java.time.temporal.WeekFields.ISO.weekOfYear()),
                        Collectors.summingDouble(Assignment::getDurationHours)));

        weeklyHours.entrySet().stream()
                .filter(entry -> entry.getValue() > maxWeekly)
                .forEach(entry -> result.addHardViolation(
                        String.format("Staff %s exceeds max weekly hours (%.1f > %.1f) in week %d",
                                getStaffName(staff), entry.getValue(), maxWeekly, entry.getKey())));

        // Monthly hours check
        Map<String, Double> monthlyHours = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().getYear() + "-" + assignment.getDate().getMonthValue(),
                        Collectors.summingDouble(Assignment::getDurationHours)));

        monthlyHours.entrySet().stream()
                .filter(entry -> entry.getValue() > maxMonthly)
                .forEach(entry -> result.addHardViolation(
                        String.format("Staff %s exceeds max monthly hours (%.1f > %.1f) in %s",
                                getStaffName(staff), entry.getValue(), maxMonthly, entry.getKey())));
    }

    /**
     * Rest period constraints
     */
    private void checkRestPeriods(Staff staff, List<Assignment> assignments,
                                  Map<String, String> constraints, ConstraintEvaluationResult result) {

        int minRestHours = parseIntConstraint(constraints, "TimeBetweenShifts", 8);
        boolean nightShiftsAllowed = parseBooleanConstraint(constraints, "NightShiftsAllowed", true);
        boolean splitShiftsAllowed = parseBooleanConstraint(constraints, "SplitShiftsAllowed", false);
        int minDayOffPerWeek = parseIntConstraint(constraints, "MinimumDayOff", 2);

        // Sort assignments chronologically
        List<Assignment> sortedAssignments = assignments.stream()
                .sorted(Comparator.comparing(Assignment::getDate)
                        .thenComparing(a -> a.getShift().getStartTime()))
                .collect(Collectors.toList());

        // Check rest between shifts
        for (int i = 0; i < sortedAssignments.size() - 1; i++) {
            Assignment current = sortedAssignments.get(i);
            Assignment next = sortedAssignments.get(i + 1);

            long hoursBetween = calculateHoursBetweenShifts(current, next);
            if (hoursBetween < minRestHours) {
                result.addHardViolation(String.format(
                        "Staff %s has insufficient rest between shifts (%.1f < %d hours) on %s-%s",
                        getStaffName(staff), (double) hoursBetween, minRestHours,
                        current.getDate(), next.getDate()));
            }
        }

        // Check night shift constraints
        if (!nightShiftsAllowed) {
            assignments.stream()
                    .filter(a -> a.getShift().getIsNightShift())
                    .forEach(a -> result.addHardViolation(String.format(
                            "Staff %s assigned to night shift but night shifts not allowed on %s",
                            getStaffName(staff), a.getDate())));
        }

        // Check split shifts
        if (!splitShiftsAllowed) {
            Map<LocalDate, Long> dailyShiftCount = assignments.stream()
                    .collect(Collectors.groupingBy(Assignment::getDate, Collectors.counting()));

            dailyShiftCount.entrySet().stream()
                    .filter(entry -> entry.getValue() > 1)
                    .forEach(entry -> result.addHardViolation(String.format(
                            "Staff %s has multiple shifts on %s but split shifts not allowed",
                            getStaffName(staff), entry.getKey())));
        }

        // Check minimum day off
        Map<Integer, List<LocalDate>> weeklyWorkDays = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().get(java.time.temporal.WeekFields.ISO.weekOfYear()),
                        Collectors.mapping(Assignment::getDate, Collectors.toList())));

        weeklyWorkDays.entrySet().stream()
                .filter(entry -> (7 - entry.getValue().stream().collect(Collectors.toSet()).size()) < minDayOffPerWeek)
                .forEach(entry -> result.addHardViolation(String.format(
                        "Staff %s has insufficient day offs in week %d (required: %d)",
                        getStaffName(staff), entry.getKey(), minDayOffPerWeek)));
    }

    /**
     * Working pattern compliance (soft constraint)
     */
    private void checkWorkingPatterns(Staff staff, List<Assignment> assignments,
                                      Map<String, String> constraints, ConstraintEvaluationResult result) {

        if (!parseBooleanConstraint(constraints, "WorkingPatternCompliance", true)) return;

        Squad squad = staff.getSquad();
        if (squad == null || squad.getSquadWorkingPattern() == null) return;

        String patternStr = squad.getSquadWorkingPattern().getShiftPattern();
        if (patternStr == null || patternStr.isBlank()) return;

        String[] patternElements = patternStr.toUpperCase().split(",");
        LocalDate anchorDate = Optional.ofNullable(squad.getStartDate())
                .orElse(assignments.get(0).getDate());

        int mismatches = 0;
        for (Assignment assignment : assignments) {
            long dayIndex = ChronoUnit.DAYS.between(anchorDate, assignment.getDate());
            if (dayIndex < 0) continue;

            String expectedPattern = patternElements[(int) (dayIndex % patternElements.length)].trim();
            if (!isPatternMatch(assignment, expectedPattern)) {
                mismatches++;
            }
        }

        if (mismatches > 0) {
            result.addSoftViolation(String.format(
                    "Staff %s has %d working pattern mismatches",
                    getStaffName(staff), mismatches));
        }
    }

    /**
     * Qualification matching (hard constraint)
     */
    private void checkQualificationMatch(Staff staff, List<Assignment> assignments,
                                         ConstraintEvaluationResult result) {

        for (Assignment assignment : assignments) {
            if (assignment.hasTask()) {
                Task task = assignment.getTask();
                boolean hasAllQualifications = task.getRequiredQualifications().stream()
                        .allMatch(required -> staff.getQualifications().contains(required));

                if (!hasAllQualifications) {
                    result.addHardViolation(String.format(
                            "Staff %s lacks required qualifications for task %s on %s",
                            getStaffName(staff), task.getName(), assignment.getDate()));
                }
            }
        }
    }

    /**
     * Day off rules compliance
     */
    private void checkDayOffRules(Staff staff, List<Assignment> assignments,
                                  ConstraintEvaluationResult result) {

        DayOffRule rule = staff.getDayOffRule();
        if (rule == null) return;

        int maxWorkingDays = Optional.ofNullable(rule.getWorkingDays()).orElse(6);

        // Check consecutive working days
        List<Assignment> sortedAssignments = assignments.stream()
                .sorted(Comparator.comparing(Assignment::getDate))
                .collect(Collectors.toList());

        int consecutiveCount = 0;
        LocalDate lastDate = null;

        for (Assignment assignment : sortedAssignments) {
            LocalDate currentDate = assignment.getDate();

            if (lastDate == null || currentDate.equals(lastDate.plusDays(1))) {
                consecutiveCount++;
            } else {
                consecutiveCount = 1;
            }

            if (consecutiveCount > maxWorkingDays) {
                result.addHardViolation(String.format(
                        "Staff %s exceeds maximum consecutive working days (%d) ending on %s",
                        getStaffName(staff), maxWorkingDays, currentDate));
                break;
            }

            lastDate = currentDate;
        }

        // Check fixed off days
        String fixedOffDays = rule.getFixedOffDays();
        if (fixedOffDays != null && !fixedOffDays.isBlank()) {
            Set<DayOfWeek> fixedDays = Arrays.stream(fixedOffDays.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> DayOfWeek.valueOf(s.toUpperCase()))
                    .collect(Collectors.toSet());

            Map<DayOfWeek, Long> workingDayCount = assignments.stream()
                    .collect(Collectors.groupingBy(a -> a.getDate().getDayOfWeek(), Collectors.counting()));

            fixedDays.stream()
                    .filter(day -> workingDayCount.getOrDefault(day, 0L) > 0)
                    .forEach(day -> result.addSoftViolation(String.format(
                            "Staff %s should be off every %s according to day off rule",
                            getStaffName(staff), day)));
        }
    }

    /**
     * Global constraint evaluation
     */
    private void evaluateGlobalConstraints(RosterPlan rosterPlan, ConstraintEvaluationResult result) {

        // Task coverage
        if (rosterPlan.getUnassignedTasks() != null && !rosterPlan.getUnassignedTasks().isEmpty()) {
            rosterPlan.getUnassignedTasks().forEach(task ->
                    result.addHardViolation(String.format("Task %s is not assigned", task.getName())));
        }

        // Overlapping assignments
        checkOverlappingAssignments(rosterPlan, result);

        // Department matching
        checkDepartmentMatching(rosterPlan, result);

        // Fairness evaluation (soft constraint)
        evaluateFairness(rosterPlan, result);
    }

    /**
     * Check for overlapping assignments
     */
    private void checkOverlappingAssignments(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        Map<Staff, List<Assignment>> staffAssignments = rosterPlan.getAssignments().stream()
                .collect(Collectors.groupingBy(Assignment::getStaff));

        for (Map.Entry<Staff, List<Assignment>> entry : staffAssignments.entrySet()) {
            Staff staff = entry.getKey();
            List<Assignment> assignments = entry.getValue();

            for (int i = 0; i < assignments.size(); i++) {
                for (int j = i + 1; j < assignments.size(); j++) {
                    Assignment a1 = assignments.get(i);
                    Assignment a2 = assignments.get(j);

                    if (a1.overlapsWith(a2)) {
                        result.addHardViolation(String.format(
                                "Staff %s has overlapping assignments on %s",
                                getStaffName(staff), a1.getDate()));
                    }
                }
            }
        }
    }

    /**
     * Check department matching
     */
    private void checkDepartmentMatching(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        rosterPlan.getAssignments().stream()
                .filter(Assignment::hasTask)
                .filter(assignment -> !assignment.getStaff().getDepartment().getId()
                        .equals(assignment.getTask().getDepartment().getId()))
                .forEach(assignment -> result.addHardViolation(String.format(
                        "Staff %s from department %s assigned to task from department %s",
                        getStaffName(assignment.getStaff()),
                        assignment.getStaff().getDepartment().getName(),
                        assignment.getTask().getDepartment().getName())));
    }

    /**
     * Fairness evaluation
     */
    private void evaluateFairness(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        Map<Staff, Double> workingHours = rosterPlan.getWorkingHoursSummary();

        if (workingHours.size() <= 1) return;

        double averageHours = workingHours.values().stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        double maxDeviation = 4.0; // Could be configurable

        workingHours.entrySet().stream()
                .filter(entry -> Math.abs(entry.getValue() - averageHours) > maxDeviation)
                .forEach(entry -> result.addSoftViolation(String.format(
                        "Staff %s has unfair workload deviation (%.1f hours from average)",
                        getStaffName(entry.getKey()),
                        Math.abs(entry.getValue() - averageHours))));
    }

    // === UTILITY METHODS ===

    private void initializeConstraintCacheIfNeeded() {
        if (!cacheInitialized) {
            synchronized (this) {
                if (!cacheInitialized) {
                    try {
                        constraintCache.clear();
                        constraintService.findAll().forEach(constraint ->
                                constraintCache.put(constraint.getName(), constraint.getDefaultValue()));
                        cacheInitialized = true;
                        log.debug("Constraint cache initialized with {} constraints", constraintCache.size());
                    } catch (Exception e) {
                        log.error("Failed to initialize constraint cache", e);
                    }
                }
            }
        }
    }

    private Map<String, String> getEffectiveConstraints(Staff staff) {
        Map<String, String> effective = new HashMap<>(constraintCache);

        // Apply staff-specific overrides
        if (staff.getConstraintOverrides() != null) {
            staff.getConstraintOverrides().stream()
                    .filter(override -> override.getActive() && override.getConstraint() != null)
                    .forEach(override -> effective.put(
                            override.getConstraint().getName(),
                            override.getOverrideValue()));
        }

        return effective;
    }

    private boolean isPatternMatch(Assignment assignment, String expectedPattern) {
        if ("DAYOFF".equals(expectedPattern) || "OFF".equals(expectedPattern)) {
            return false; // Assignment exists, so not a day off
        }

        Shift shift = assignment.getShift();
        if (shift == null) return false;

        String shiftName = shift.getName().toUpperCase();
        String shiftId = shift.getId().toString();

        return shiftName.contains(expectedPattern) ||
                shiftId.equals(expectedPattern) ||
                expectedPattern.contains(shiftName);
    }

    private long calculateHoursBetweenShifts(Assignment current, Assignment next) {
        LocalTime currentEnd = current.getShift().getEndTime();
        LocalTime nextStart = next.getShift().getStartTime();

        if (current.getDate().equals(next.getDate())) {
            return ChronoUnit.HOURS.between(currentEnd, nextStart);
        } else if (current.getDate().plusDays(1).equals(next.getDate())) {
            long hoursToMidnight = ChronoUnit.HOURS.between(currentEnd, LocalTime.MIDNIGHT);
            long hoursFromMidnight = ChronoUnit.HOURS.between(LocalTime.MIDNIGHT, nextStart);
            return hoursToMidnight + hoursFromMidnight;
        }

        return 24; // Sufficient rest if more than one day between assignments
    }

    private String getStaffName(Staff staff) {
        return staff.getName() + " " + staff.getSurname();
    }

    private double parseDoubleConstraint(Map<String, String> constraints, String key, double defaultValue) {
        try {
            return Double.parseDouble(constraints.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Invalid constraint value for {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    private int parseIntConstraint(Map<String, String> constraints, String key, int defaultValue) {
        try {
            return Integer.parseInt(constraints.getOrDefault(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            log.warn("Invalid constraint value for {}, using default: {}", key, defaultValue);
            return defaultValue;
        }
    }

    private boolean parseBooleanConstraint(Map<String, String> constraints, String key, boolean defaultValue) {
        return Boolean.parseBoolean(constraints.getOrDefault(key, String.valueOf(defaultValue)));
    }

    public void clearConstraintCache() {
        constraintCache.clear();
        cacheInitialized = false;
        log.debug("Constraint cache cleared");
    }
}
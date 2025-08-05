package com.rosteroptimization.service.optimization.constraint;

import com.rosteroptimization.entity.*;
import com.rosteroptimization.service.ConstraintService;
import com.rosteroptimization.service.optimization.model.Assignment;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Evaluates constraint violations for roster plans and assignments
 * Acts as a bridge between optimization algorithms and existing constraint services
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConstraintEvaluator {

    private final ConstraintService constraintService;

    // Cache for constraints to avoid repeated database queries - static to ensure singleton
    private static Map<String, String> globalConstraintsCache = null;

    /**
     * Evaluate all constraints for a complete roster plan
     */
    public ConstraintEvaluationResult evaluateRosterPlan(RosterPlan rosterPlan) {
        log.debug("Evaluating constraints for roster plan: {}", rosterPlan.getPlanId());

        ConstraintEvaluationResult result = new ConstraintEvaluationResult();

        // Initialize constraint cache once per evaluation
        initializeConstraintCache();

        // Group assignments by staff ID to avoid hashCode issues with Hibernate proxies
        Map<Long, List<Assignment>> assignmentsByStaffId = rosterPlan.getAssignments().stream()
                .collect(Collectors.groupingBy(assignment -> assignment.getStaff().getId()));

        // Evaluate constraints for each staff member
        for (Map.Entry<Long, List<Assignment>> entry : assignmentsByStaffId.entrySet()) {
            List<Assignment> staffAssignments = entry.getValue();
            
            // Get staff from first assignment (all assignments have same staff)
            Staff staff = staffAssignments.get(0).getStaff();

            evaluateStaffConstraints(staff, staffAssignments, result);
        }

        // Evaluate global constraints
        evaluateGlobalConstraints(rosterPlan, result);

        log.debug("Constraint evaluation completed. Hard violations: {}, Soft violations: {}",
                result.getHardViolationCount(), result.getSoftViolationCount());

        return result;
    }

    /**
     * Clear constraint cache (useful for long-running optimization processes)
     */
    public void clearConstraintCache() {
        globalConstraintsCache = null;
        log.debug("Constraint cache cleared");
    }

    /**
     * Evaluate constraints for a single staff member's assignments
     */
    private void evaluateStaffConstraints(Staff staff, List<Assignment> assignments,
                                          ConstraintEvaluationResult result) {

        // Get effective constraint values for this staff (considering overrides)
        Map<String, String> effectiveConstraints = getEffectiveConstraintsForStaff(staff);

        // Max working hours per day
        evaluateMaxWorkingHoursPerDay(staff, assignments, effectiveConstraints, result);

        // Max working hours per week
        evaluateMaxWorkingHoursPerWeek(staff, assignments, effectiveConstraints, result);

        // Max working hours per month
        evaluateMaxWorkingHoursPerMonth(staff, assignments, effectiveConstraints, result);

        // Minimum day off constraints
        evaluateMinimumDayOff(staff, assignments, effectiveConstraints, result);

        // Time between shifts
        evaluateTimeBetweenShifts(staff, assignments, effectiveConstraints, result);

        // Night shift constraints
        evaluateNightShiftConstraints(staff, assignments, effectiveConstraints, result);

        // Split shifts constraint
        evaluateSplitShiftsConstraint(staff, assignments, effectiveConstraints, result);

        // Working pattern compliance (soft constraint)
        evaluateWorkingPatternCompliance(staff, assignments, effectiveConstraints, result);

        // Qualification match (hard constraint)
        evaluateQualificationMatch(staff, assignments, result);

        // Day off rules compliance
        evaluateDayOffRules(staff, assignments, result);
    }

    /**
     * Evaluate global/cross-staff constraints
     */
    private void evaluateGlobalConstraints(RosterPlan rosterPlan, ConstraintEvaluationResult result) {

        // Task coverage enforcement
        evaluateTaskCoverage(rosterPlan, result);

        // Overlapping assignments
        evaluateOverlappingAssignments(rosterPlan, result);

        // Department match
        evaluateDepartmentMatch(rosterPlan, result);

        // Fairness (soft constraint)
        evaluateFairness(rosterPlan, result);
    }

    /**
     * Max working hours per day constraint
     */
    private void evaluateMaxWorkingHoursPerDay(Staff staff, List<Assignment> assignments,
                                               Map<String, String> constraints, ConstraintEvaluationResult result) {

        double maxHours = Double.parseDouble(constraints.getOrDefault("MaxWorkingHoursPerDay", "12"));

        Map<LocalDate, Double> dailyHours = assignments.stream()
                .collect(Collectors.groupingBy(
                        Assignment::getDate,
                        Collectors.summingDouble(Assignment::getDurationHours)
                ));

        for (Map.Entry<LocalDate, Double> entry : dailyHours.entrySet()) {
            if (entry.getValue() > maxHours) {
                result.addHardViolation(String.format("Staff %s exceeds max daily hours (%.1f > %.1f) on %s",
                        staff.getName() + " " + staff.getSurname(), entry.getValue(), maxHours, entry.getKey()));
            }
        }
    }

    /**
     * Max working hours per week constraint
     */
    private void evaluateMaxWorkingHoursPerWeek(Staff staff, List<Assignment> assignments,
                                                Map<String, String> constraints, ConstraintEvaluationResult result) {

        double maxHours = Double.parseDouble(constraints.getOrDefault("MaxWorkingHoursPerWeek", "40"));

        // Group by week and calculate total hours
        Map<Integer, Double> weeklyHours = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().get(java.time.temporal.WeekFields.ISO.weekOfYear()),
                        Collectors.summingDouble(Assignment::getDurationHours)
                ));

        for (Map.Entry<Integer, Double> entry : weeklyHours.entrySet()) {
            if (entry.getValue() > maxHours) {
                result.addHardViolation(String.format("Staff %s exceeds max weekly hours (%.1f > %.1f) in week %d",
                        staff.getName() + " " + staff.getSurname(), entry.getValue(), maxHours, entry.getKey()));
            }
        }
    }

    /**
     * Max working hours per month constraint
     */
    private void evaluateMaxWorkingHoursPerMonth(Staff staff, List<Assignment> assignments,
                                                 Map<String, String> constraints, ConstraintEvaluationResult result) {

        double maxHours = Double.parseDouble(constraints.getOrDefault("MaxWorkingHoursPerMonth", "160"));

        // Group by month and calculate total hours
        Map<String, Double> monthlyHours = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().getYear() + "-" + assignment.getDate().getMonthValue(),
                        Collectors.summingDouble(Assignment::getDurationHours)
                ));

        for (Map.Entry<String, Double> entry : monthlyHours.entrySet()) {
            if (entry.getValue() > maxHours) {
                result.addHardViolation(String.format("Staff %s exceeds max monthly hours (%.1f > %.1f) in %s",
                        staff.getName() + " " + staff.getSurname(), entry.getValue(), maxHours, entry.getKey()));
            }
        }
    }

    /**
     * Time between shifts constraint
     */
    private void evaluateTimeBetweenShifts(Staff staff, List<Assignment> assignments,
                                           Map<String, String> constraints, ConstraintEvaluationResult result) {

        int minHoursBetween = Integer.parseInt(constraints.getOrDefault("TimeBetweenShifts", "8"));

        // Sort assignments by date and time
        List<Assignment> sortedAssignments = assignments.stream()
                .sorted((a1, a2) -> {
                    int dateCompare = a1.getDate().compareTo(a2.getDate());
                    if (dateCompare != 0) return dateCompare;
                    return a1.getShift().getStartTime().compareTo(a2.getShift().getStartTime());
                })
                .toList();

        for (int i = 0; i < sortedAssignments.size() - 1; i++) {
            Assignment current = sortedAssignments.get(i);
            Assignment next = sortedAssignments.get(i + 1);

            long hoursBetween = calculateHoursBetweenShifts(current, next);

            if (hoursBetween < minHoursBetween) {
                result.addHardViolation(String.format("Staff %s has insufficient rest between shifts (%.1f < %d hours)",
                        staff.getName() + " " + staff.getSurname(), (double) hoursBetween, minHoursBetween));
            }
        }
    }

    /**
     * Qualification match constraint
     */
    private void evaluateQualificationMatch(Staff staff, List<Assignment> assignments,
                                            ConstraintEvaluationResult result) {

        for (Assignment assignment : assignments) {
            if (assignment.hasTask()) {
                Task task = assignment.getTask();

                // Check if staff has all required qualifications for the task
                boolean hasAllQualifications = task.getRequiredQualifications().stream()
                        .allMatch(required -> staff.getQualifications().contains(required));

                if (!hasAllQualifications) {
                    result.addHardViolation(String.format("Staff %s lacks required qualifications for task %s",
                            staff.getName() + " " + staff.getSurname(), task.getName()));
                }
            }
        }
    }

    /**
     * Working pattern compliance (soft constraint)
     */
    private void evaluateWorkingPatternCompliance(Staff staff, List<Assignment> assignments,
                                                  Map<String, String> constraints, ConstraintEvaluationResult result) {

        boolean complianceEnabled = Boolean.parseBoolean(constraints.getOrDefault("WorkingPatternCompliance", "true"));

        if (!complianceEnabled) return;

        try {
            Squad squad = staff.getSquad();
            if (squad == null) {
                log.debug("Staff has no squad assigned");
                return;
            }
            
            // Check if squad working pattern is available (should be eagerly fetched now)
            if (squad.getSquadWorkingPattern() == null) {
                log.debug("Squad has no working pattern assigned");
                return;
            }

            // TODO: Implement working pattern compliance logic
            // This would check if staff assignments match their squad's working pattern

            log.debug("Working pattern compliance evaluation not fully implemented yet");
        } catch (Exception e) {
            log.warn("Error evaluating working pattern compliance for staff {}: {}", 
                     staff.getId(), e.getMessage());
            // Continue processing without failing the entire evaluation
        }
    }

    /**
     * Get effective constraint values for staff (considering overrides)
     */
    /**
     * Initialize constraint cache to avoid repeated database queries
     */
    private void initializeConstraintCache() {
        if (globalConstraintsCache == null) {
            log.debug("Initializing constraint cache");
            globalConstraintsCache = constraintService.findAll().stream()
                    .collect(Collectors.toMap(
                            constraint -> constraint.getName(),
                            constraint -> constraint.getDefaultValue()
                    ));
            log.debug("Constraint cache initialized with {} constraints", globalConstraintsCache.size());
        }
    }

    private Map<String, String> getEffectiveConstraintsForStaff(Staff staff) {
        // Use cached global constraints
        Map<String, String> effectiveConstraints = new java.util.HashMap<>(globalConstraintsCache);

        // Apply staff-specific overrides - only if staff has constraint overrides
        if (staff.getConstraintOverrides() != null && !staff.getConstraintOverrides().isEmpty()) {
            staff.getConstraintOverrides().stream()
                    .filter(override -> override.getConstraint() != null && override.getActive())
                    .forEach(override -> effectiveConstraints.put(
                            override.getConstraint().getName(),
                            override.getOverrideValue()
                    ));
        }

        return effectiveConstraints;
    }

    /**
     * Calculate hours between two consecutive shifts
     */
    private long calculateHoursBetweenShifts(Assignment current, Assignment next) {
        LocalTime currentEnd = current.getShift().getEndTime();
        LocalTime nextStart = next.getShift().getStartTime();

        // If assignments are on the same day
        if (current.getDate().equals(next.getDate())) {
            return ChronoUnit.HOURS.between(currentEnd, nextStart);
        }

        // If assignments are on consecutive days
        if (current.getDate().plusDays(1).equals(next.getDate())) {
            // Calculate hours from current end time to midnight, then from midnight to next start
            long hoursToMidnight = ChronoUnit.HOURS.between(currentEnd, LocalTime.MIDNIGHT);
            long hoursFromMidnight = ChronoUnit.HOURS.between(LocalTime.MIDNIGHT, nextStart);
            return hoursToMidnight + hoursFromMidnight;
        }

        // If there are days between assignments, there's enough rest time
        return 24; // Return a value greater than typical minimum rest time
    }

    /**
     * Night shift constraints
     */
    private void evaluateNightShiftConstraints(Staff staff, List<Assignment> assignments,
                                               Map<String, String> constraints, ConstraintEvaluationResult result) {

        boolean nightShiftsAllowed = Boolean.parseBoolean(constraints.getOrDefault("NightShiftsAllowed", "true"));

        if (nightShiftsAllowed) return;

        for (Assignment assignment : assignments) {
            if (assignment.getShift().getIsNightShift()) {
                result.addHardViolation(String.format("Staff %s assigned to night shift but night shifts not allowed",
                        staff.getName() + " " + staff.getSurname()));
            }
        }
    }

    /**
     * Split shifts constraint (multiple shifts on same day)
     */
    private void evaluateSplitShiftsConstraint(Staff staff, List<Assignment> assignments,
                                               Map<String, String> constraints, ConstraintEvaluationResult result) {

        boolean splitShiftsAllowed = Boolean.parseBoolean(constraints.getOrDefault("SplitShiftsAllowed", "false"));

        if (splitShiftsAllowed) return;

        Map<LocalDate, Long> shiftsPerDay = assignments.stream()
                .collect(Collectors.groupingBy(Assignment::getDate, Collectors.counting()));

        for (Map.Entry<LocalDate, Long> entry : shiftsPerDay.entrySet()) {
            if (entry.getValue() > 1) {
                result.addHardViolation(String.format("Staff %s has multiple shifts on %s but split shifts not allowed",
                        staff.getName() + " " + staff.getSurname(), entry.getKey()));
            }
        }
    }

    /**
     * Minimum day off constraint
     */
    private void evaluateMinimumDayOff(Staff staff, List<Assignment> assignments,
                                       Map<String, String> constraints, ConstraintEvaluationResult result) {

        int minDayOffPerWeek = Integer.parseInt(constraints.getOrDefault("MinimumDayOff", "2"));

        // Group assignments by week
        Map<Integer, List<LocalDate>> workDaysByWeek = assignments.stream()
                .collect(Collectors.groupingBy(
                        assignment -> assignment.getDate().get(java.time.temporal.WeekFields.ISO.weekOfYear()),
                        Collectors.mapping(Assignment::getDate, Collectors.toList())
                ));

        for (Map.Entry<Integer, List<LocalDate>> entry : workDaysByWeek.entrySet()) {
            int workDaysInWeek = (int) entry.getValue().stream().distinct().count();
            int dayOffInWeek = 7 - workDaysInWeek;

            if (dayOffInWeek < minDayOffPerWeek) {
                result.addHardViolation(String.format("Staff %s has insufficient day offs in week %d (%d < %d)",
                        staff.getName() + " " + staff.getSurname(), entry.getKey(), dayOffInWeek, minDayOffPerWeek));
            }
        }
    }

    /**
     * Day off rules compliance
     */
    private void evaluateDayOffRules(Staff staff, List<Assignment> assignments, ConstraintEvaluationResult result) {
        DayOffRule dayOffRule = staff.getDayOffRule();
        if (dayOffRule == null) return;

        // TODO: Implement day off rules logic based on WORKING_DAYS, OFF_DAYS, and FIXED_OFF_DAYS
        // This would check compliance with staff-specific day off patterns

        log.debug("Day off rules evaluation not fully implemented yet for staff: {}", staff.getName());
    }

    /**
     * Task coverage enforcement
     */
    private void evaluateTaskCoverage(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        // Check if all tasks are assigned
        if (rosterPlan.getUnassignedTasks() != null && !rosterPlan.getUnassignedTasks().isEmpty()) {
            for (Task unassignedTask : rosterPlan.getUnassignedTasks()) {
                result.addHardViolation(String.format("Task %s is not assigned to any staff member",
                        unassignedTask.getName()));
            }
        }
    }

    /**
     * Overlapping assignments
     */
    private void evaluateOverlappingAssignments(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        // Group assignments by staff
        Map<Staff, List<Assignment>> assignmentsByStaff = rosterPlan.getAssignments().stream()
                .collect(Collectors.groupingBy(Assignment::getStaff));

        for (Map.Entry<Staff, List<Assignment>> entry : assignmentsByStaff.entrySet()) {
            Staff staff = entry.getKey();
            List<Assignment> staffAssignments = entry.getValue();

            // Check for overlaps within staff assignments
            for (int i = 0; i < staffAssignments.size(); i++) {
                for (int j = i + 1; j < staffAssignments.size(); j++) {
                    Assignment a1 = staffAssignments.get(i);
                    Assignment a2 = staffAssignments.get(j);

                    if (a1.overlapsWith(a2)) {
                        result.addHardViolation(String.format("Staff %s has overlapping assignments on %s",
                                staff.getName() + " " + staff.getSurname(), a1.getDate()));
                    }
                }
            }
        }
    }

    /**
     * Department match constraint
     */
    private void evaluateDepartmentMatch(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        for (Assignment assignment : rosterPlan.getAssignments()) {
            if (assignment.hasTask()) {
                Staff staff = assignment.getStaff();
                Task task = assignment.getTask();

                if (!staff.getDepartment().getId().equals(task.getDepartment().getId())) {
                    result.addHardViolation(String.format("Staff %s from department %s assigned to task from department %s",
                            staff.getName() + " " + staff.getSurname(),
                            staff.getDepartment().getName(),
                            task.getDepartment().getName()));
                }
            }
        }
    }

    /**
     * Fairness evaluation (soft constraint)
     */
    private void evaluateFairness(RosterPlan rosterPlan, ConstraintEvaluationResult result) {
        Map<Staff, Double> workingHours = rosterPlan.getWorkingHoursSummary();

        if (workingHours.size() <= 1) return;

        double averageHours = workingHours.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double maxDeviation = 4.0; // Could be configurable

        for (Map.Entry<Staff, Double> entry : workingHours.entrySet()) {
            double deviation = Math.abs(entry.getValue() - averageHours);
            if (deviation > maxDeviation) {
                result.addSoftViolation(String.format("Staff %s has unfair workload deviation (%.1f hours from average)",
                        entry.getKey().getName() + " " + entry.getKey().getSurname(), deviation));
            }
        }
    }
}
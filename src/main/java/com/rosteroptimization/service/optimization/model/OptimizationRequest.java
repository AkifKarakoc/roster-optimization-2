package com.rosteroptimization.service.optimization.model;

import com.rosteroptimization.entity.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Contains all input data needed for roster optimization
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationRequest {

    // Planning period
    private LocalDate startDate;
    private LocalDate endDate;

    // Core entities
    private List<Staff> staffList;
    private List<Task> taskList;
    private List<Shift> shiftList;
    private Department department;

    // Constraints
    private List<Constraint> globalConstraints;
    private Map<Long, List<ConstraintOverride>> staffConstraintOverrides; // staffId -> overrides

    // Algorithm parameters (algorithm-specific configurations)
    private Map<String, Object> algorithmParameters;

    // Optimization settings
    private String algorithmType; // "GENETIC_ALGORITHM", "SIMULATED_ANNEALING", etc.
    private int maxExecutionTimeMinutes;
    private boolean enableParallelProcessing;

    /**
     * Get number of planning days
     */
    public int getPlanningDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return (int) startDate.until(endDate).getDays() + 1;
    }

    /**
     * Get active staff members only
     */
    public List<Staff> getActiveStaff() {
        return staffList.stream()
                .filter(staff -> staff.getActive())
                .toList();
    }

    /**
     * Get active tasks only
     */
    public List<Task> getActiveTasks() {
        return taskList.stream()
                .filter(task -> task.getActive())
                .toList();
    }

    /**
     * Get active shifts only
     */
    public List<Shift> getActiveShifts() {
        return shiftList.stream()
                .filter(shift -> shift.getActive())
                .toList();
    }

    /**
     * Get constraint overrides for specific staff member
     */
    public List<ConstraintOverride> getConstraintOverridesForStaff(Long staffId) {
        return staffConstraintOverrides.getOrDefault(staffId, List.of());
    }

    /**
     * Check if algorithm parameter exists
     */
    public boolean hasAlgorithmParameter(String key) {
        return algorithmParameters != null && algorithmParameters.containsKey(key);
    }

    /**
     * Get algorithm parameter with default value
     */
    @SuppressWarnings("unchecked")
    public <T> T getAlgorithmParameter(String key, T defaultValue) {
        if (algorithmParameters == null || !algorithmParameters.containsKey(key)) {
            return defaultValue;
        }
        return (T) algorithmParameters.get(key);
    }

    /**
     * Validate the optimization request
     */
    public void validate() {
        if (startDate == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (staffList == null || staffList.isEmpty()) {
            throw new IllegalArgumentException("Staff list cannot be empty");
        }
        if (shiftList == null || shiftList.isEmpty()) {
            throw new IllegalArgumentException("Shift list cannot be empty");
        }
        if (department == null) {
            throw new IllegalArgumentException("Department is required");
        }
        if (algorithmType == null || algorithmType.trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm type is required");
        }
    }
}
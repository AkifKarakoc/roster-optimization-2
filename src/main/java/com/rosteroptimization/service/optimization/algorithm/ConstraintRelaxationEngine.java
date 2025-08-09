package com.rosteroptimization.service.optimization.algorithm;

import com.rosteroptimization.entity.Task;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ConstraintRelaxationEngine {

    public boolean isProblemInfeasible(OptimizationRequest request) {
        log.debug("Checking feasibility for {} staff, {} tasks",
                request.getActiveStaff().size(), request.getActiveTasks().size());

        // Simple capacity check
        double totalTaskHours = calculateTotalTaskHours(request.getActiveTasks());
        double totalStaffCapacity = calculateTotalStaffCapacity(request.getActiveStaff(), request.getPlanningDays());

        double utilizationRate = totalStaffCapacity > 0 ? totalTaskHours / totalStaffCapacity : Double.MAX_VALUE;
        boolean capacityShortage = utilizationRate > 0.95;

        // Department mismatch check
        Set<Long> taskDepartments = request.getActiveTasks().stream()
                .map(task -> task.getDepartment().getId())
                .collect(Collectors.toSet());

        Set<Long> staffDepartments = request.getActiveStaff().stream()
                .map(staff -> staff.getDepartment().getId())
                .collect(Collectors.toSet());

        boolean departmentMismatch = !staffDepartments.containsAll(taskDepartments);

        // Qualification gap check
        boolean qualificationGap = hasQualificationGaps(request);

        boolean infeasible = capacityShortage || departmentMismatch || qualificationGap;

        if (infeasible) {
            log.warn("Problem infeasible: capacity={}, departments={}, qualifications={}",
                    capacityShortage, departmentMismatch, qualificationGap);
        }

        return infeasible;
    }

    public OptimizationRequest applyMinimalRelaxation(OptimizationRequest originalRequest) {
        log.info("Applying minimal constraint relaxation");

        // Strategy: Remove lowest priority tasks
        List<Task> filteredTasks = removeLowestPriorityTasks(originalRequest.getActiveTasks());

        OptimizationRequest relaxedRequest = OptimizationRequest.builder()
                .startDate(originalRequest.getStartDate())
                .endDate(originalRequest.getEndDate())
                .staffList(originalRequest.getStaffList())
                .taskList(filteredTasks)
                .shiftList(originalRequest.getShiftList())
                .department(originalRequest.getDepartment())
                .globalConstraints(originalRequest.getGlobalConstraints())
                .staffConstraintOverrides(originalRequest.getStaffConstraintOverrides())
                .algorithmParameters(addRelaxationMetadata(originalRequest.getAlgorithmParameters()))
                .algorithmType(originalRequest.getAlgorithmType())
                .maxExecutionTimeMinutes(originalRequest.getMaxExecutionTimeMinutes())
                .enableParallelProcessing(originalRequest.isEnableParallelProcessing())
                .build();

        log.info("Relaxation applied: {} tasks removed ({}→{})",
                originalRequest.getActiveTasks().size() - filteredTasks.size(),
                originalRequest.getActiveTasks().size(), filteredTasks.size());

        return relaxedRequest;
    }

    private boolean hasQualificationGaps(OptimizationRequest request) {
        Set<String> requiredQualifications = request.getActiveTasks().stream()
                .flatMap(task -> task.getRequiredQualifications().stream())
                .map(qual -> qual.getName())
                .collect(Collectors.toSet());

        Set<String> availableQualifications = request.getActiveStaff().stream()
                .flatMap(staff -> staff.getQualifications().stream())
                .map(qual -> qual.getName())
                .collect(Collectors.toSet());

        return !availableQualifications.containsAll(requiredQualifications);
    }

    private List<Task> removeLowestPriorityTasks(List<Task> originalTasks) {
        if (originalTasks.size() <= 5) return originalTasks; // Keep minimum tasks

        // Remove 20% of lowest priority tasks
        int tasksToRemove = (int) Math.ceil(originalTasks.size() * 0.2);

        List<Task> sortedTasks = originalTasks.stream()
                .sorted(Comparator.comparingInt(Task::getPriority)) // Lower number = higher priority
                .collect(Collectors.toList());

        int keepCount = originalTasks.size() - tasksToRemove;
        return sortedTasks.subList(0, keepCount);
    }

    private Map<String, Object> addRelaxationMetadata(Map<String, Object> originalParams) {
        Map<String, Object> newParams = new HashMap<>(originalParams != null ? originalParams : new HashMap<>());
        newParams.put("constraintRelaxationApplied", true);
        newParams.put("relaxationStrategy", "REMOVE_LOWEST_PRIORITY_TASKS");
        return newParams;
    }

    private double calculateTotalTaskHours(List<Task> tasks) {
        return tasks.stream()
                .mapToDouble(task -> {
                    long minutes = java.time.Duration.between(task.getStartTime(), task.getEndTime()).toMinutes();
                    return minutes / 60.0;
                })
                .sum();
    }

    private double calculateTotalStaffCapacity(List<Staff> staff, int planningDays) {
        return staff.size() * planningDays * 8.0; // 8 hours per day max
    }
}
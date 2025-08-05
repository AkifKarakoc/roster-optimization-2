package com.rosteroptimization.service;

import com.rosteroptimization.entity.*;
import com.rosteroptimization.repository.*;
import com.rosteroptimization.service.optimization.algorithm.Optimizer;
import com.rosteroptimization.service.optimization.algorithm.OptimizationException;
import com.rosteroptimization.service.optimization.model.OptimizationRequest;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for roster optimization and management
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RosterService {

    private final StaffRepository staffRepository;
    private final TaskRepository taskRepository;
    private final ShiftRepository shiftRepository;
    private final DepartmentRepository departmentRepository;
    private final ConstraintRepository constraintRepository;
    private final ConstraintOverrideRepository constraintOverrideRepository;

    // Optimizer implementations (Spring will inject all available optimizers)
    private final List<Optimizer> optimizers;

    /**
     * Generate a new roster plan using specified algorithm
     */
    public RosterPlan generateRosterPlan(RosterGenerationRequest request) throws OptimizationException {
        log.info("Generating roster plan for department {} from {} to {} using {}",
                request.getDepartmentId(), request.getStartDate(), request.getEndDate(), request.getAlgorithmType());

        // Validate request
        validateRosterGenerationRequest(request);

        // Get optimizer for specified algorithm
        Optimizer optimizer = getOptimizer(request.getAlgorithmType());

        // Build optimization request
        OptimizationRequest optimizationRequest = buildOptimizationRequest(request);

        // Run optimization
        RosterPlan rosterPlan = optimizer.optimize(optimizationRequest);

        log.info("Roster plan generated successfully. {} assignments, fitness: {:.2f}, feasible: {}",
                rosterPlan.getAssignments().size(), rosterPlan.getFitnessScore(), rosterPlan.isFeasible());

        return rosterPlan;
    }

    /**
     * Get list of available optimization algorithms
     */
    public List<AlgorithmInfo> getAvailableAlgorithms() {
        return optimizers.stream()
                .map(optimizer -> new AlgorithmInfo(
                        optimizer.getAlgorithmName(),
                        optimizer.getAlgorithmDescription(),
                        optimizer.supportsParallelProcessing(),
                        optimizer.getDefaultParameters(),
                        optimizer.getConfigurableParameters()
                ))
                .toList();
    }

    /**
     * Get estimated execution time for optimization request
     */
    public long getEstimatedExecutionTime(RosterGenerationRequest request) {
        try {
            Optimizer optimizer = getOptimizer(request.getAlgorithmType());
            OptimizationRequest optimizationRequest = buildOptimizationRequest(request);
            return optimizer.getEstimatedExecutionTime(optimizationRequest);
        } catch (Exception e) {
            log.warn("Could not estimate execution time: {}", e.getMessage());
            return 60000; // Default 1 minute
        }
    }

    /**
     * Validate roster plan (check constraints without optimization)
     */
    @Transactional(readOnly = true)
    public RosterValidationResult validateRosterPlan(RosterPlan rosterPlan) {
        // This would use ConstraintEvaluator to validate an existing plan
        log.info("Validating roster plan: {}", rosterPlan.getPlanId());

        // TODO: Implement validation logic
        return new RosterValidationResult(true, new ArrayList<>(), new ArrayList<>());
    }

    /**
     * Get roster statistics for analysis
     */
    @Transactional(readOnly = true)
    public RosterStatistics getRosterStatistics(RosterPlan rosterPlan) {
        log.info("Calculating statistics for roster plan: {}", rosterPlan.getPlanId());

        RosterStatistics stats = new RosterStatistics();

        // Basic statistics
        stats.setTotalAssignments(rosterPlan.getAssignments().size());
        stats.setTotalWorkingHours(rosterPlan.getAssignments().stream()
                .mapToDouble(assignment -> assignment.getDurationHours())
                .sum());

        // Staff statistics
        Map<String, Double> staffWorkingHours = rosterPlan.getWorkingHoursSummary().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getName() + " " + entry.getKey().getSurname(),
                        Map.Entry::getValue
                ));
        stats.setStaffWorkingHours(staffWorkingHours);

        // Coverage statistics
        stats.setTaskCoverageRate(rosterPlan.getTaskCoverageRate());
        stats.setStaffUtilizationRate(rosterPlan.getStaffUtilizationRate());

        // Constraint violations
        stats.setHardConstraintViolations(rosterPlan.getHardConstraintViolations());
        stats.setSoftConstraintViolations(rosterPlan.getSoftConstraintViolations());

        // Daily distribution
        Map<String, Integer> dailyAssignments = rosterPlan.getAssignmentsPerDay().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toString(),
                        entry -> entry.getValue().intValue()
                ));
        stats.setDailyAssignments(dailyAssignments);

        return stats;
    }

    /**
     * Build optimization request from roster generation request
     */
    private OptimizationRequest buildOptimizationRequest(RosterGenerationRequest request) {
        // Get department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId()));

        // Get staff in department
        List<Staff> departmentStaff = staffRepository.findByDepartmentIdAndActiveTrue(department.getId());

        // Get tasks for department and date range
        LocalDateTime startDateTime = request.getStartDate().atStartOfDay();
        LocalDateTime endDateTime = request.getEndDate().atTime(23, 59, 59);
        List<Task> departmentTasks = taskRepository.findByDepartmentIdAndActiveTrueAndStartTimeBetween(
                department.getId(), startDateTime, endDateTime);

        // Get all active shifts
        List<Shift> activeShifts = shiftRepository.findByActiveTrue();

        // Get global constraints
        List<Constraint> globalConstraints = constraintRepository.findByActiveTrue();

        // Get constraint overrides for department staff efficiently
        List<Long> staffIds = departmentStaff.stream().map(Staff::getId).toList();
        List<ConstraintOverride> departmentStaffOverrides = constraintOverrideRepository
                .findByStaffIdInAndConstraintActiveTrueAndActiveTrue(staffIds);
        
        Map<Long, List<ConstraintOverride>> staffOverrides = departmentStaffOverrides.stream()
                .collect(java.util.stream.Collectors.groupingBy(override -> override.getStaff().getId()));

        return OptimizationRequest.builder()
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .staffList(departmentStaff)
                .taskList(departmentTasks)
                .shiftList(activeShifts)
                .department(department)
                .globalConstraints(globalConstraints)
                .staffConstraintOverrides(staffOverrides)
                .algorithmType(request.getAlgorithmType())
                .algorithmParameters(request.getAlgorithmParameters())
                .maxExecutionTimeMinutes(request.getMaxExecutionTimeMinutes())
                .enableParallelProcessing(request.isEnableParallelProcessing())
                .build();
    }

    /**
     * Get optimizer by algorithm type
     */
    private Optimizer getOptimizer(String algorithmType) throws OptimizationException {
        return optimizers.stream()
                .filter(optimizer -> optimizer.getAlgorithmName().equals(algorithmType))
                .findFirst()
                .orElseThrow(() -> new OptimizationException("Unknown algorithm type: " + algorithmType));
    }

    /**
     * Validate roster generation request
     */
    private void validateRosterGenerationRequest(RosterGenerationRequest request) {
        if (request.getDepartmentId() == null) {
            throw new IllegalArgumentException("Department ID is required");
        }
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        if (request.getAlgorithmType() == null || request.getAlgorithmType().trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm type is required");
        }
        if (request.getStartDate().until(request.getEndDate()).getDays() > 31) {
            throw new IllegalArgumentException("Planning period cannot exceed 31 days");
        }
    }

    /**
     * Data classes for service responses
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    public static class RosterGenerationRequest {
        private Long departmentId;
        private LocalDate startDate;
        private LocalDate endDate;
        private String algorithmType;
        private Map<String, Object> algorithmParameters = new HashMap<>();
        private int maxExecutionTimeMinutes = 10;
        private boolean enableParallelProcessing = true;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class AlgorithmInfo {
        private String name;
        private String description;
        private boolean supportsParallelProcessing;
        private Map<String, Object> defaultParameters;
        private Map<String, Optimizer.ParameterInfo> configurableParameters;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RosterValidationResult {
        private boolean valid;
        private List<String> hardViolations;
        private List<String> softViolations;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    public static class RosterStatistics {
        private int totalAssignments;
        private double totalWorkingHours;
        private Map<String, Double> staffWorkingHours;
        private double taskCoverageRate;
        private double staffUtilizationRate;
        private int hardConstraintViolations;
        private int softConstraintViolations;
        private Map<String, Integer> dailyAssignments;
    }
}
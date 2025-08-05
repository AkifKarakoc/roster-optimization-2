package com.rosteroptimization.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for RosterPlan to avoid Jackson serialization issues
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RosterPlanDTO {

    private String planId;
    private LocalDateTime generatedAt;
    private String algorithmUsed;
    private LocalDate startDate;
    private LocalDate endDate;

    // Simplified assignments
    private List<AssignmentDTO> assignments;

    // Optimization results
    private double fitnessScore;
    private int hardConstraintViolations;
    private int softConstraintViolations;
    private long executionTimeMs;
    private boolean feasible;

    // Unassigned items (simplified)
    private List<TaskDTO> unassignedTasks;
    private List<StaffDTO> underutilizedStaff;

    // Statistics
    private Map<String, Object> statistics;
    private Map<String, Object> algorithmMetadata;

    // Summary statistics
    private int totalAssignments;
    private int uniqueStaffCount;
    private double taskCoverageRate;
    private double staffUtilizationRate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignmentDTO {
        private StaffDTO staff;
        private ShiftDTO shift;
        private TaskDTO task;
        private LocalDate date;
        private double durationHours;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffDTO {
        private Long id;
        private String name;
        private String surname;
        private String registrationCode;
        private String title;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftDTO {
        private Long id;
        private String name;
        private String startTime;
        private String endTime;
        private String type;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDTO {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String priority;
        private int requiredStaffCount;
    }
}
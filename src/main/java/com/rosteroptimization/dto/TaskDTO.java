package com.rosteroptimization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDTO {

    private Long id;

    @NotBlank(message = "Task name is required")
    @Size(max = 100, message = "Task name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @NotNull(message = "Priority is required")
    @Min(value = 1, message = "Priority must be at least 1")
    @Max(value = 10, message = "Priority cannot exceed 10")
    private Integer priority;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active = true;

    // Foreign Key references
    @NotNull(message = "Department is required")
    private Long departmentId;

    // Required qualification IDs (for M2M relationship)
    private Set<Long> requiredQualificationIds;

    // Nested objects (for response only)
    private DepartmentDTO department;
    private Set<QualificationDTO> requiredQualifications;

    // Computed fields (for response only)
    private String durationHours; // Duration in HH:mm format
    private Integer durationMinutes; // Duration in minutes
    private String priorityLevel; // HIGH, MEDIUM, LOW based on priority value
    private String status; // UPCOMING, ONGOING, COMPLETED based on current time
    private Integer requiredQualificationCount;
    private Boolean crossesMidnight; // True if task crosses midnight
    private String dateDisplay; // Formatted date for display
    private String timeDisplay; // Formatted time range for display
}
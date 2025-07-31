package com.rosteroptimization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDTO {

    private Long id;

    @NotBlank(message = "Shift name is required")
    @Size(max = 100, message = "Shift name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private Boolean isNightShift = false;

    private Boolean fixed = true;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active = true;

    // Foreign Key references
    @NotNull(message = "Working period is required")
    private Long workingPeriodId;

    // Nested objects (for response only)
    private WorkingPeriodDTO workingPeriod;

    // Computed fields (for response only)
    private String durationHours; // Duration in HH:mm format
    private Boolean crossesMidnight; // True if shift crosses midnight
}
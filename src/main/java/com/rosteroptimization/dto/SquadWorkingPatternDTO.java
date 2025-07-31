package com.rosteroptimization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquadWorkingPatternDTO {

    private Long id;

    @NotBlank(message = "Squad working pattern name is required")
    @Size(max = 100, message = "Squad working pattern name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Shift pattern is required")
    @Size(max = 1000, message = "Shift pattern cannot exceed 1000 characters")
    private String shiftPattern; // Comma-separated: "Shift_1,DayOff,Shift_4,DayOff"

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Cycle length is required")
    @Min(value = 1, message = "Cycle length must be at least 1 day")
    @Max(value = 365, message = "Cycle length cannot exceed 365 days")
    private Integer cycleLength;

    private Boolean active = true;

    // Statistics fields (for response only)
    private Long squadCount;

    // Computed fields (for response only)
    private List<String> patternItems; // Parsed pattern items
    private Integer workingDaysInCycle; // Number of working days (non-DayOff)
    private Integer dayOffCount; // Number of day-off days
    private Double workingDaysPercentage; // Percentage of working days in cycle

    // Validation fields (for response only)
    private Boolean isValidPattern; // True if all shifts exist and pattern is valid
    private List<String> patternErrors; // List of validation errors
}
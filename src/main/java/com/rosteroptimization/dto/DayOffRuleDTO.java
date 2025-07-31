package com.rosteroptimization.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DayOffRuleDTO {

    private Long id;

    @NotNull(message = "Working days is required")
    @Min(value = 1, message = "Working days must be at least 1")
    @Max(value = 14, message = "Working days cannot exceed 14")
    private Integer workingDays;

    @NotNull(message = "Off days is required")
    @Min(value = 1, message = "Off days must be at least 1")
    @Max(value = 7, message = "Off days cannot exceed 7")
    private Integer offDays;

    @Size(max = 100, message = "Fixed off days cannot exceed 100 characters")
    @Pattern(regexp = "^$|^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)(,(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY))*$",
            message = "Fixed off days must be comma-separated day names (e.g., MONDAY,TUESDAY)")
    private String fixedOffDays;

    // Foreign Key reference
    @NotNull(message = "Staff is required")
    private Long staffId;

    // Nested objects (for response only)
    private StaffDTO staff;

    // Computed fields (for response only)
    private String ruleType; // FLEXIBLE or FIXED based on fixedOffDays
    private List<String> fixedOffDaysList; // Parsed fixed off days as list
    private Integer totalCycleDays; // workingDays + offDays
    private Double workRatio; // workingDays / totalCycleDays percentage
    private Boolean hasFixedDays; // True if fixedOffDays is not empty
    private String ruleDescription; // Human-readable description of the rule
}
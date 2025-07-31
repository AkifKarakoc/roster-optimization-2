package com.rosteroptimization.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SquadDTO {

    private Long id;

    @NotBlank(message = "Squad name is required")
    @Size(max = 100, message = "Squad name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active = true;

    // Foreign Key references
    @NotNull(message = "Squad working pattern is required")
    private Long squadWorkingPatternId;

    // Nested objects (for response only)
    private SquadWorkingPatternDTO squadWorkingPattern;

    // Statistics fields (for response only)
    private Long staffCount;

    // Computed fields (for response only)
    private Integer daysSinceStart; // Days since squad started
    private Integer currentCycleDay; // Current day in the working pattern cycle (1-based)
    private String currentCyclePosition; // Current shift or "DayOff" based on pattern
    private Boolean isNewSquad; // True if squad started less than 30 days ago
}
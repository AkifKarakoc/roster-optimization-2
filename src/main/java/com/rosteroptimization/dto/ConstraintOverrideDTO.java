package com.rosteroptimization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintOverrideDTO {

    private Long id;

    @NotBlank(message = "Override value is required")
    @Size(max = 100, message = "Override value cannot exceed 100 characters")
    private String overrideValue;

    // Foreign Key references
    @NotNull(message = "Staff is required")
    private Long staffId;

    @NotNull(message = "Constraint is required")
    private Long constraintId;

    // Nested objects (for response only)
    private StaffDTO staff;
    private ConstraintDTO constraint;

    // Display fields (for response only)
    private String staffDisplayName; // Staff surname, name
    private String staffRegistrationCode; // Staff registration code
    private String constraintName; // Constraint name
    private String constraintType; // HARD or SOFT
    private String defaultValue; // Original constraint default value
    private Boolean isDifferent; // True if overrideValue != defaultValue
}
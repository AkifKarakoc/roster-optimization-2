package com.rosteroptimization.dto;

import com.rosteroptimization.entity.Constraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConstraintDTO {

    private Long id;

    @NotBlank(message = "Constraint name is required")
    @Size(max = 100, message = "Constraint name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Constraint type is required")
    private Constraint.ConstraintType type;

    @NotBlank(message = "Default value is required")
    @Size(max = 100, message = "Default value cannot exceed 100 characters")
    private String defaultValue;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active = true;

    // Statistics fields (for response only)
    private Long overrideCount;
    private Long staffWithOverridesCount;

    // Display fields (for response only)
    private String typeDisplay; // HARD or SOFT as display text
    private String valueType; // Detected value type (BOOLEAN, INTEGER, DECIMAL, TEXT)
    private Boolean isNumeric; // True if defaultValue is numeric
    private Boolean isBoolean; // True if defaultValue is boolean
}
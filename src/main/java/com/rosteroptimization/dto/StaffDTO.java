package com.rosteroptimization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Set;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StaffDTO {

    private Long id;

    @NotBlank(message = "Staff name is required")
    @Size(max = 100, message = "Staff name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Staff surname is required")
    @Size(max = 100, message = "Staff surname cannot exceed 100 characters")
    private String surname;

    @NotBlank(message = "Registration code is required")
    @Size(max = 50, message = "Registration code cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Registration code can only contain letters, numbers, underscore and dash")
    private String registrationCode;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Pattern(regexp = "^[+]?[0-9\\s\\-\\(\\)]*$", message = "Invalid phone number format")
    private String phone;

    private Boolean active = true;

    // Foreign Key references
    @NotNull(message = "Department is required")
    private Long departmentId;

    @NotNull(message = "Squad is required")
    private Long squadId;

    // Qualification IDs (for M2M relationship)
    private Set<Long> qualificationIds;

    // Nested objects (for response only)
    private DepartmentDTO department;
    private SquadDTO squad;
    private Set<QualificationDTO> qualifications;
    private DayOffRuleDTO dayOffRule;
    private List<ConstraintOverrideDTO> constraintOverrides;

    // Computed fields (for response only)
    private String fullName; // name + surname
    private String displayName; // surname, name
    private Integer qualificationCount;
    private Boolean hasDayOffRule;
    private Boolean hasConstraintOverrides;
    private String currentCyclePosition; // Current shift/dayoff from squad pattern
}
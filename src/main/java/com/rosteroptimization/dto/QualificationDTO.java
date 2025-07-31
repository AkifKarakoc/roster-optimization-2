package com.rosteroptimization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualificationDTO {

    private Long id;

    @NotBlank(message = "Qualification name is required")
    @Size(max = 100, message = "Qualification name cannot exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private Boolean active = true;

    // Statistics fields (for response only, not for create/update)
    private Long staffCount;
    private Long taskCount;
}
package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.ConstraintOverrideDTO;
import com.rosteroptimization.entity.ConstraintOverride;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.entity.Constraint;
import com.rosteroptimization.repository.StaffRepository;
import com.rosteroptimization.repository.ConstraintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ConstraintOverrideMapper {

    private final StaffRepository staffRepository;
    private final ConstraintRepository constraintRepository;
    @Lazy
    @Autowired
    private StaffMapper staffMapper;
    private final ConstraintMapper constraintMapper;

    /**
     * Convert Entity to DTO
     */
    public ConstraintOverrideDTO toDto(ConstraintOverride entity) {
        if (entity == null) {
            return null;
        }

        ConstraintOverrideDTO dto = new ConstraintOverrideDTO();
        dto.setId(entity.getId());
        dto.setOverrideValue(entity.getOverrideValue());

        // Set foreign key references
        if (entity.getStaff() != null) {
            dto.setStaffId(entity.getStaff().getId());
            dto.setStaff(staffMapper.toDto(entity.getStaff()));
        }

        if (entity.getConstraint() != null) {
            dto.setConstraintId(entity.getConstraint().getId());
            dto.setConstraint(constraintMapper.toDto(entity.getConstraint()));
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public ConstraintOverride toEntity(ConstraintOverrideDTO dto) {
        if (dto == null) {
            return null;
        }

        ConstraintOverride entity = new ConstraintOverride();
        entity.setId(dto.getId());
        entity.setOverrideValue(dto.getOverrideValue());

        // Set foreign key relationships
        if (dto.getStaffId() != null) {
            Staff staff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));
            entity.setStaff(staff);
        }

        if (dto.getConstraintId() != null) {
            Constraint constraint = constraintRepository.findById(dto.getConstraintId())
                    .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + dto.getConstraintId()));
            entity.setConstraint(constraint);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(ConstraintOverrideDTO dto, ConstraintOverride entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setOverrideValue(dto.getOverrideValue());

        // Update foreign key relationships
        if (dto.getStaffId() != null) {
            Staff staff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));
            entity.setStaff(staff);
        }

        if (dto.getConstraintId() != null) {
            Constraint constraint = constraintRepository.findById(dto.getConstraintId())
                    .orElseThrow(() -> new IllegalArgumentException("Constraint not found with ID: " + dto.getConstraintId()));
            entity.setConstraint(constraint);
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<ConstraintOverrideDTO> toDtoList(List<ConstraintOverride> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for constraint override
     */
    private void calculateComputedFields(ConstraintOverride entity, ConstraintOverrideDTO dto) {
        // Staff display fields
        if (entity.getStaff() != null) {
            dto.setStaffDisplayName(entity.getStaff().getSurname() + ", " + entity.getStaff().getName());
            dto.setStaffRegistrationCode(entity.getStaff().getRegistrationCode());
        }

        // Constraint display fields
        if (entity.getConstraint() != null) {
            dto.setConstraintName(entity.getConstraint().getName());
            dto.setConstraintType(entity.getConstraint().getType().toString());
            dto.setDefaultValue(entity.getConstraint().getDefaultValue());

            // Check if override value is different from default
            if (entity.getOverrideValue() != null && entity.getConstraint().getDefaultValue() != null) {
                dto.setIsDifferent(!entity.getOverrideValue().equals(entity.getConstraint().getDefaultValue()));
            } else {
                dto.setIsDifferent(entity.getOverrideValue() != null || entity.getConstraint().getDefaultValue() != null);
            }
        }
    }

    /**
     * Create override DTO with basic info (for bulk operations)
     */
    public ConstraintOverrideDTO toBasicDto(ConstraintOverride entity) {
        if (entity == null) {
            return null;
        }

        ConstraintOverrideDTO dto = new ConstraintOverrideDTO();
        dto.setId(entity.getId());
        dto.setOverrideValue(entity.getOverrideValue());

        if (entity.getStaff() != null) {
            dto.setStaffId(entity.getStaff().getId());
            dto.setStaffDisplayName(entity.getStaff().getSurname() + ", " + entity.getStaff().getName());
            dto.setStaffRegistrationCode(entity.getStaff().getRegistrationCode());
        }

        if (entity.getConstraint() != null) {
            dto.setConstraintId(entity.getConstraint().getId());
            dto.setConstraintName(entity.getConstraint().getName());
            dto.setConstraintType(entity.getConstraint().getType().toString());
            dto.setDefaultValue(entity.getConstraint().getDefaultValue());
        }

        return dto;
    }

    /**
     * Validate constraint override value based on constraint type
     */
    public boolean isValidOverrideValue(String overrideValue, Constraint constraint) {
        if (constraint == null || overrideValue == null) {
            return false;
        }

        // For now, we just check if the value is not empty
        // In a real implementation, you might want to validate based on constraint-specific rules
        return !overrideValue.trim().isEmpty();
    }
}
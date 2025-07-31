package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.ConstraintDTO;
import com.rosteroptimization.entity.Constraint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ConstraintMapper {

    /**
     * Convert Entity to DTO
     */
    public ConstraintDTO toDto(Constraint entity) {
        if (entity == null) {
            return null;
        }

        ConstraintDTO dto = new ConstraintDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setDefaultValue(entity.getDefaultValue());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Calculate statistics if relationships are loaded
        if (entity.getConstraintOverrides() != null) {
            dto.setOverrideCount((long) entity.getConstraintOverrides().size());
            dto.setStaffWithOverridesCount((long) entity.getConstraintOverrides().stream()
                    .map(override -> override.getStaff().getId())
                    .distinct()
                    .count());
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Constraint toEntity(ConstraintDTO dto) {
        if (dto == null) {
            return null;
        }

        Constraint entity = new Constraint();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setDefaultValue(dto.getDefaultValue());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(ConstraintDTO dto, Constraint entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setType(dto.getType());
        entity.setDefaultValue(dto.getDefaultValue());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<ConstraintDTO> toDtoList(List<Constraint> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for constraint
     */
    private void calculateComputedFields(Constraint entity, ConstraintDTO dto) {
        // Type display
        if (entity.getType() != null) {
            dto.setTypeDisplay(entity.getType().toString());
        }

        // Value type detection
        if (StringUtils.hasText(entity.getDefaultValue())) {
            dto.setValueType(detectValueType(entity.getDefaultValue()));
            dto.setIsNumeric(isNumeric(entity.getDefaultValue()));
            dto.setIsBoolean(isBoolean(entity.getDefaultValue()));
        }
    }

    /**
     * Detect value type from string
     */
    private String detectValueType(String value) {
        if (!StringUtils.hasText(value)) {
            return "TEXT";
        }

        String trimmedValue = value.trim().toLowerCase();

        // Check boolean
        if ("true".equals(trimmedValue) || "false".equals(trimmedValue) ||
                "yes".equals(trimmedValue) || "no".equals(trimmedValue) ||
                "enabled".equals(trimmedValue) || "disabled".equals(trimmedValue)) {
            return "BOOLEAN";
        }

        // Check integer
        try {
            Long.parseLong(trimmedValue);
            return "INTEGER";
        } catch (NumberFormatException e) {
            // Not an integer
        }

        // Check decimal
        try {
            Double.parseDouble(trimmedValue);
            return "DECIMAL";
        } catch (NumberFormatException e) {
            // Not a decimal
        }

        return "TEXT";
    }

    /**
     * Check if value is numeric
     */
    private Boolean isNumeric(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Check if value is boolean
     */
    private Boolean isBoolean(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String trimmedValue = value.trim().toLowerCase();
        return "true".equals(trimmedValue) || "false".equals(trimmedValue) ||
                "yes".equals(trimmedValue) || "no".equals(trimmedValue) ||
                "enabled".equals(trimmedValue) || "disabled".equals(trimmedValue);
    }

    /**
     * Parse boolean value from string
     */
    public Boolean parseBooleanValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String trimmedValue = value.trim().toLowerCase();
        switch (trimmedValue) {
            case "true", "yes", "enabled":
                return true;
            case "false", "no", "disabled":
                return false;
            default:
                throw new IllegalArgumentException("Cannot parse boolean value: " + value);
        }
    }

    /**
     * Parse integer value from string
     */
    public Integer parseIntegerValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse integer value: " + value);
        }
    }

    /**
     * Parse double value from string
     */
    public Double parseDoubleValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot parse double value: " + value);
        }
    }
}
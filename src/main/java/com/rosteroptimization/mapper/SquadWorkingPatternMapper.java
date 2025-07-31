package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.SquadWorkingPatternDTO;
import com.rosteroptimization.entity.SquadWorkingPattern;
import com.rosteroptimization.repository.ShiftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SquadWorkingPatternMapper {

    private final ShiftRepository shiftRepository;
    private static final String DAY_OFF = "DayOff";

    /**
     * Convert Entity to DTO
     */
    public SquadWorkingPatternDTO toDto(SquadWorkingPattern entity) {
        if (entity == null) {
            return null;
        }

        SquadWorkingPatternDTO dto = new SquadWorkingPatternDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setShiftPattern(entity.getShiftPattern());
        dto.setDescription(entity.getDescription());
        dto.setCycleLength(entity.getCycleLength());
        dto.setActive(entity.getActive());

        // Calculate statistics if relationships are loaded
        if (entity.getSquadList() != null) {
            dto.setSquadCount((long) entity.getSquadList().size());
        }

        // Parse and validate pattern
        parseAndValidatePattern(entity.getShiftPattern(), entity.getCycleLength(), dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public SquadWorkingPattern toEntity(SquadWorkingPatternDTO dto) {
        if (dto == null) {
            return null;
        }

        SquadWorkingPattern entity = new SquadWorkingPattern();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setShiftPattern(dto.getShiftPattern());
        entity.setDescription(dto.getDescription());
        entity.setCycleLength(dto.getCycleLength());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(SquadWorkingPatternDTO dto, SquadWorkingPattern entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setShiftPattern(dto.getShiftPattern());
        entity.setDescription(dto.getDescription());
        entity.setCycleLength(dto.getCycleLength());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<SquadWorkingPatternDTO> toDtoList(List<SquadWorkingPattern> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Parse and validate shift pattern
     */
    private void parseAndValidatePattern(String shiftPattern, Integer cycleLength, SquadWorkingPatternDTO dto) {
        List<String> errors = new ArrayList<>();
        List<String> patternItems = new ArrayList<>();
        int workingDays = 0;
        int dayOffDays = 0;

        if (!StringUtils.hasText(shiftPattern)) {
            errors.add("Shift pattern cannot be empty");
            dto.setIsValidPattern(false);
            dto.setPatternErrors(errors);
            return;
        }

        // Parse pattern items
        String[] items = shiftPattern.split(",");
        for (String item : items) {
            String trimmedItem = item.trim();
            patternItems.add(trimmedItem);

            if (DAY_OFF.equalsIgnoreCase(trimmedItem)) {
                dayOffDays++;
            } else {
                workingDays++;
                // Validate shift exists (optional - can be expensive for large datasets)
                if (!isValidShiftName(trimmedItem)) {
                    errors.add("Invalid shift reference: " + trimmedItem);
                }
            }
        }

        // Validate cycle length matches pattern length
        if (cycleLength != null && patternItems.size() != cycleLength) {
            errors.add("Pattern length (" + patternItems.size() + ") does not match cycle length (" + cycleLength + ")");
        }

        // Set computed fields
        dto.setPatternItems(patternItems);
        dto.setWorkingDaysInCycle(workingDays);
        dto.setDayOffCount(dayOffDays);

        if (patternItems.size() > 0) {
            dto.setWorkingDaysPercentage((double) workingDays / patternItems.size() * 100);
        } else {
            dto.setWorkingDaysPercentage(0.0);
        }

        dto.setIsValidPattern(errors.isEmpty());
        dto.setPatternErrors(errors.isEmpty() ? null : errors);
    }

    /**
     * Validate if shift name is valid (basic validation)
     */
    private boolean isValidShiftName(String shiftName) {
        if (!StringUtils.hasText(shiftName)) {
            return false;
        }

        // Basic validation - should start with "Shift_" or be a valid identifier
        // For performance, we do basic pattern check rather than database lookup
        return shiftName.matches("^[a-zA-Z0-9_-]+$") && shiftName.length() <= 50;
    }

    /**
     * Validate pattern format and content
     */
    public List<String> validatePattern(String shiftPattern, Integer cycleLength) {
        List<String> errors = new ArrayList<>();
        SquadWorkingPatternDTO tempDto = new SquadWorkingPatternDTO();
        parseAndValidatePattern(shiftPattern, cycleLength, tempDto);
        return tempDto.getPatternErrors() != null ? tempDto.getPatternErrors() : new ArrayList<>();
    }
}
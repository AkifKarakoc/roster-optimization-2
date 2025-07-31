package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.DayOffRuleDTO;
import com.rosteroptimization.entity.DayOffRule;
import com.rosteroptimization.entity.Staff;
import com.rosteroptimization.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DayOffRuleMapper {

    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;

    /**
     * Convert Entity to DTO
     */
    public DayOffRuleDTO toDto(DayOffRule entity) {
        if (entity == null) {
            return null;
        }

        DayOffRuleDTO dto = new DayOffRuleDTO();
        dto.setId(entity.getId());
        dto.setWorkingDays(entity.getWorkingDays());
        dto.setOffDays(entity.getOffDays());
        dto.setFixedOffDays(entity.getFixedOffDays());

        // Set foreign key references
        if (entity.getStaff() != null) {
            dto.setStaffId(entity.getStaff().getId());
            dto.setStaff(staffMapper.toDto(entity.getStaff()));
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public DayOffRule toEntity(DayOffRuleDTO dto) {
        if (dto == null) {
            return null;
        }

        DayOffRule entity = new DayOffRule();
        entity.setId(dto.getId());
        entity.setWorkingDays(dto.getWorkingDays());
        entity.setOffDays(dto.getOffDays());
        entity.setFixedOffDays(dto.getFixedOffDays());

        // Set foreign key relationships
        if (dto.getStaffId() != null) {
            Staff staff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));
            entity.setStaff(staff);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(DayOffRuleDTO dto, DayOffRule entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setWorkingDays(dto.getWorkingDays());
        entity.setOffDays(dto.getOffDays());
        entity.setFixedOffDays(dto.getFixedOffDays());

        // Update foreign key relationships
        if (dto.getStaffId() != null) {
            Staff staff = staffRepository.findById(dto.getStaffId())
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + dto.getStaffId()));
            entity.setStaff(staff);
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<DayOffRuleDTO> toDtoList(List<DayOffRule> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for day off rule
     */
    private void calculateComputedFields(DayOffRule entity, DayOffRuleDTO dto) {
        // Rule type
        dto.setRuleType(StringUtils.hasText(entity.getFixedOffDays()) ? "FIXED" : "FLEXIBLE");
        dto.setHasFixedDays(StringUtils.hasText(entity.getFixedOffDays()));

        // Parse fixed off days
        if (StringUtils.hasText(entity.getFixedOffDays())) {
            List<String> fixedDays = Arrays.stream(entity.getFixedOffDays().split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            dto.setFixedOffDaysList(fixedDays);
        } else {
            dto.setFixedOffDaysList(Collections.emptyList());
        }

        // Cycle calculations
        if (entity.getWorkingDays() != null && entity.getOffDays() != null) {
            int totalCycle = entity.getWorkingDays() + entity.getOffDays();
            dto.setTotalCycleDays(totalCycle);

            if (totalCycle > 0) {
                double workRatio = (double) entity.getWorkingDays() / totalCycle * 100;
                dto.setWorkRatio(Math.round(workRatio * 100.0) / 100.0); // Round to 2 decimal places
            }
        }

        // Rule description
        dto.setRuleDescription(generateRuleDescription(entity));
    }

    /**
     * Generate human-readable rule description
     */
    private String generateRuleDescription(DayOffRule entity) {
        if (entity.getWorkingDays() == null || entity.getOffDays() == null) {
            return "Invalid rule";
        }

        StringBuilder description = new StringBuilder();
        description.append("Work ").append(entity.getWorkingDays()).append(" day(s)");
        description.append(", then ").append(entity.getOffDays()).append(" day(s) off");

        if (StringUtils.hasText(entity.getFixedOffDays())) {
            description.append(" (Fixed off days: ").append(entity.getFixedOffDays()).append(")");
        }

        return description.toString();
    }

    /**
     * Validate fixed off days format
     */
    public boolean isValidFixedOffDays(String fixedOffDays) {
        if (!StringUtils.hasText(fixedOffDays)) {
            return true; // Empty is valid (flexible rule)
        }

        String[] validDays = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
        String[] inputDays = fixedOffDays.split(",");

        for (String day : inputDays) {
            String trimmedDay = day.trim().toUpperCase();
            boolean isValid = Arrays.stream(validDays).anyMatch(validDay -> validDay.equals(trimmedDay));
            if (!isValid) {
                return false;
            }
        }

        return true;
    }

    /**
     * Parse fixed off days string to list
     */
    public List<String> parseFixedOffDays(String fixedOffDays) {
        if (!StringUtils.hasText(fixedOffDays)) {
            return Collections.emptyList();
        }

        return Arrays.stream(fixedOffDays.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }
}
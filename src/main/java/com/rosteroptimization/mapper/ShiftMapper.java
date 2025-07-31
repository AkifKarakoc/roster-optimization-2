package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.ShiftDTO;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.WorkingPeriod;
import com.rosteroptimization.repository.WorkingPeriodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ShiftMapper {

    private final WorkingPeriodRepository workingPeriodRepository;
    private final WorkingPeriodMapper workingPeriodMapper;

    /**
     * Convert Entity to DTO
     */
    public ShiftDTO toDto(Shift entity) {
        if (entity == null) {
            return null;
        }

        ShiftDTO dto = new ShiftDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setIsNightShift(entity.getIsNightShift());
        dto.setFixed(entity.getFixed());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Set foreign key references
        if (entity.getWorkingPeriod() != null) {
            dto.setWorkingPeriodId(entity.getWorkingPeriod().getId());
            dto.setWorkingPeriod(workingPeriodMapper.toDto(entity.getWorkingPeriod()));
        }

        // Calculate computed fields
        dto.setDurationHours(calculateDuration(entity.getStartTime(), entity.getEndTime()));
        dto.setCrossesMidnight(crossesMidnight(entity.getStartTime(), entity.getEndTime()));

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Shift toEntity(ShiftDTO dto) {
        if (dto == null) {
            return null;
        }

        Shift entity = new Shift();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setIsNightShift(dto.getIsNightShift() != null ? dto.getIsNightShift() : false);
        entity.setFixed(dto.getFixed() != null ? dto.getFixed() : true);
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Set foreign key relationships
        if (dto.getWorkingPeriodId() != null) {
            WorkingPeriod workingPeriod = workingPeriodRepository.findById(dto.getWorkingPeriodId())
                    .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + dto.getWorkingPeriodId()));
            entity.setWorkingPeriod(workingPeriod);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(ShiftDTO dto, Shift entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setIsNightShift(dto.getIsNightShift() != null ? dto.getIsNightShift() : false);
        entity.setFixed(dto.getFixed() != null ? dto.getFixed() : true);
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Update foreign key relationships
        if (dto.getWorkingPeriodId() != null) {
            WorkingPeriod workingPeriod = workingPeriodRepository.findById(dto.getWorkingPeriodId())
                    .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + dto.getWorkingPeriodId()));
            entity.setWorkingPeriod(workingPeriod);
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<ShiftDTO> toDtoList(List<Shift> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate duration between start and end time
     */
    private String calculateDuration(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return "0:00";
        }

        Duration duration;
        if (endTime.isBefore(startTime)) {
            // Next day calculation (crosses midnight)
            duration = Duration.between(startTime, LocalTime.MIDNIGHT)
                    .plus(Duration.between(LocalTime.MIDNIGHT, endTime));
        } else {
            duration = Duration.between(startTime, endTime);
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return String.format("%d:%02d", hours, minutes);
    }

    /**
     * Check if shift crosses midnight
     */
    private Boolean crossesMidnight(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isBefore(startTime);
    }
}
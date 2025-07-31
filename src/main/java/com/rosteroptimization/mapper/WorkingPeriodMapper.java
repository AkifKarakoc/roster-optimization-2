package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.WorkingPeriodDTO;
import com.rosteroptimization.entity.WorkingPeriod;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WorkingPeriodMapper {

    /**
     * Convert Entity to DTO
     */
    public WorkingPeriodDTO toDto(WorkingPeriod entity) {
        if (entity == null) {
            return null;
        }

        WorkingPeriodDTO dto = new WorkingPeriodDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartTime(entity.getStartTime());
        dto.setEndTime(entity.getEndTime());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Calculate statistics if relationships are loaded
        if (entity.getShiftList() != null) {
            dto.setShiftCount((long) entity.getShiftList().size());
        }

        // Calculate computed fields
        dto.setDurationHours(calculateDuration(entity.getStartTime(), entity.getEndTime()));
        dto.setIsNightPeriod(isNightPeriod(entity.getStartTime(), entity.getEndTime()));

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public WorkingPeriod toEntity(WorkingPeriodDTO dto) {
        if (dto == null) {
            return null;
        }

        WorkingPeriod entity = new WorkingPeriod();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(WorkingPeriodDTO dto, WorkingPeriod entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setStartTime(dto.getStartTime());
        entity.setEndTime(dto.getEndTime());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<WorkingPeriodDTO> toDtoList(List<WorkingPeriod> entities) {
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
     * Check if period crosses midnight
     */
    private Boolean isNightPeriod(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isBefore(startTime);
    }
}
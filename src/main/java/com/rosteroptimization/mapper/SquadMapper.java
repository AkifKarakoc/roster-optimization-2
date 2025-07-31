package com.rosteroptimization.mapper;

import com.rosteroptimization.dto.SquadDTO;
import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.entity.SquadWorkingPattern;
import com.rosteroptimization.repository.SquadWorkingPatternRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SquadMapper {

    private final SquadWorkingPatternRepository squadWorkingPatternRepository;
    private final SquadWorkingPatternMapper squadWorkingPatternMapper;

    /**
     * Convert Entity to DTO
     */
    public SquadDTO toDto(Squad entity) {
        if (entity == null) {
            return null;
        }

        SquadDTO dto = new SquadDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStartDate(entity.getStartDate());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.getActive());

        // Set foreign key references
        if (entity.getSquadWorkingPattern() != null) {
            dto.setSquadWorkingPatternId(entity.getSquadWorkingPattern().getId());
            dto.setSquadWorkingPattern(squadWorkingPatternMapper.toDto(entity.getSquadWorkingPattern()));
        }

        // Calculate statistics if relationships are loaded
        if (entity.getStaffList() != null) {
            dto.setStaffCount((long) entity.getStaffList().size());
        }

        // Calculate computed fields
        calculateComputedFields(entity, dto);

        return dto;
    }

    /**
     * Convert DTO to Entity
     */
    public Squad toEntity(SquadDTO dto) {
        if (dto == null) {
            return null;
        }

        Squad entity = new Squad();
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setStartDate(dto.getStartDate());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Set foreign key relationships
        if (dto.getSquadWorkingPatternId() != null) {
            SquadWorkingPattern squadWorkingPattern = squadWorkingPatternRepository.findById(dto.getSquadWorkingPatternId())
                    .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + dto.getSquadWorkingPatternId()));
            entity.setSquadWorkingPattern(squadWorkingPattern);
        }

        return entity;
    }

    /**
     * Update existing entity with DTO data
     */
    public void updateEntityFromDto(SquadDTO dto, Squad entity) {
        if (dto == null || entity == null) {
            return;
        }

        entity.setName(dto.getName());
        entity.setStartDate(dto.getStartDate());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.getActive() != null ? dto.getActive() : true);

        // Update foreign key relationships
        if (dto.getSquadWorkingPatternId() != null) {
            SquadWorkingPattern squadWorkingPattern = squadWorkingPatternRepository.findById(dto.getSquadWorkingPatternId())
                    .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + dto.getSquadWorkingPatternId()));
            entity.setSquadWorkingPattern(squadWorkingPattern);
        }
    }

    /**
     * Convert Entity List to DTO List
     */
    public List<SquadDTO> toDtoList(List<Squad> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Calculate computed fields for squad
     */
    private void calculateComputedFields(Squad entity, SquadDTO dto) {
        if (entity.getStartDate() == null) {
            return;
        }

        LocalDate today = LocalDate.now();

        // Calculate days since start
        long daysSinceStart = ChronoUnit.DAYS.between(entity.getStartDate(), today);
        dto.setDaysSinceStart((int) daysSinceStart);

        // Check if new squad (less than 30 days)
        dto.setIsNewSquad(daysSinceStart < 30);

        // Calculate current cycle position
        if (entity.getSquadWorkingPattern() != null && entity.getSquadWorkingPattern().getCycleLength() != null) {
            calculateCyclePosition(entity, dto, daysSinceStart);
        }
    }

    /**
     * Calculate current position in working pattern cycle
     */
    private void calculateCyclePosition(Squad entity, SquadDTO dto, long daysSinceStart) {
        SquadWorkingPattern pattern = entity.getSquadWorkingPattern();
        Integer cycleLength = pattern.getCycleLength();
        String shiftPattern = pattern.getShiftPattern();

        if (cycleLength == null || cycleLength <= 0 || shiftPattern == null) {
            return;
        }

        // Calculate current day in cycle (0-based index)
        int currentCycleDayIndex = (int) (daysSinceStart % cycleLength);
        dto.setCurrentCycleDay(currentCycleDayIndex + 1); // Convert to 1-based for display

        // Parse pattern and get current position
        String[] patternItems = shiftPattern.split(",");
        if (currentCycleDayIndex < patternItems.length) {
            String currentPosition = patternItems[currentCycleDayIndex].trim();
            dto.setCurrentCyclePosition(currentPosition);
        }
    }

    /**
     * Get current cycle position for a squad on a specific date
     */
    public String getCurrentCyclePosition(Squad squad, LocalDate date) {
        if (squad == null || squad.getStartDate() == null || date == null) {
            return null;
        }

        if (date.isBefore(squad.getStartDate())) {
            return null; // Squad hasn't started yet
        }

        SquadWorkingPattern pattern = squad.getSquadWorkingPattern();
        if (pattern == null || pattern.getCycleLength() == null || pattern.getShiftPattern() == null) {
            return null;
        }

        long daysSinceStart = ChronoUnit.DAYS.between(squad.getStartDate(), date);
        int currentCycleDayIndex = (int) (daysSinceStart % pattern.getCycleLength());

        String[] patternItems = pattern.getShiftPattern().split(",");
        if (currentCycleDayIndex < patternItems.length) {
            return patternItems[currentCycleDayIndex].trim();
        }

        return null;
    }
}
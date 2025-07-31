package com.rosteroptimization.service;

import com.rosteroptimization.dto.SquadWorkingPatternDTO;
import com.rosteroptimization.entity.SquadWorkingPattern;
import com.rosteroptimization.mapper.SquadWorkingPatternMapper;
import com.rosteroptimization.repository.SquadWorkingPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SquadWorkingPatternService {

    private final SquadWorkingPatternRepository squadWorkingPatternRepository;
    private final SquadWorkingPatternMapper squadWorkingPatternMapper;

    /**
     * Create new squad working pattern
     */
    public SquadWorkingPatternDTO create(SquadWorkingPatternDTO dto) {
        log.info("Creating new squad working pattern: {}", dto.getName());

        // Validate pattern data
        validatePatternData(dto);

        // Check if squad working pattern with same name already exists
        Optional<SquadWorkingPattern> existing = squadWorkingPatternRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Squad working pattern with name '" + dto.getName() + "' already exists");
        }

        SquadWorkingPattern entity = squadWorkingPatternMapper.toEntity(dto);
        SquadWorkingPattern saved = squadWorkingPatternRepository.save(entity);

        log.info("Squad working pattern created with ID: {}", saved.getId());
        return squadWorkingPatternMapper.toDto(saved);
    }

    /**
     * Update existing squad working pattern
     */
    public SquadWorkingPatternDTO update(Long id, SquadWorkingPatternDTO dto) {
        log.info("Updating squad working pattern with ID: {}", id);

        // Validate pattern data
        validatePatternData(dto);

        SquadWorkingPattern existing = squadWorkingPatternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + id));

        // Check if another squad working pattern with same name exists
        Optional<SquadWorkingPattern> duplicateName = squadWorkingPatternRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Squad working pattern with name '" + dto.getName() + "' already exists");
        }

        squadWorkingPatternMapper.updateEntityFromDto(dto, existing);
        SquadWorkingPattern updated = squadWorkingPatternRepository.save(existing);

        log.info("Squad working pattern updated: {}", updated.getName());
        return squadWorkingPatternMapper.toDto(updated);
    }

    /**
     * Soft delete squad working pattern
     */
    public void delete(Long id) {
        log.info("Soft deleting squad working pattern with ID: {}", id);

        SquadWorkingPattern squadWorkingPattern = squadWorkingPatternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + id));

        // Check if pattern is being used by any active squads
        long squadCount = squadWorkingPatternRepository.countSquadsUsingPattern(id);
        if (squadCount > 0) {
            throw new IllegalArgumentException("Cannot delete squad working pattern that is being used by " + squadCount + " active squad(s)");
        }

        squadWorkingPattern.setActive(false);
        squadWorkingPatternRepository.save(squadWorkingPattern);

        log.info("Squad working pattern soft deleted: {}", squadWorkingPattern.getName());
    }

    /**
     * Find squad working pattern by ID
     */
    @Transactional(readOnly = true)
    public SquadWorkingPatternDTO findById(Long id) {
        SquadWorkingPattern squadWorkingPattern = squadWorkingPatternRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + id));

        return squadWorkingPatternMapper.toDto(squadWorkingPattern);
    }

    /**
     * Find all active squad working patterns
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> findAll() {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.findByActiveTrueOrderByNameAsc();
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Find all squad working patterns (including inactive)
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> findAllIncludingInactive() {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.findAllByOrderByNameAsc();
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Search squad working patterns by name
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> searchByName(String name, boolean includeInactive) {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.searchByNameContaining(name, !includeInactive);
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns by cycle length
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> findByCycleLength(Integer cycleLength) {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.findByCycleLengthAndActiveTrue(cycleLength);
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns containing specific shift
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> findByShiftName(String shiftName) {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.findByShiftPatternContaining(shiftName);
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns by cycle length range
     */
    @Transactional(readOnly = true)
    public List<SquadWorkingPatternDTO> findByCycleLengthRange(Integer minLength, Integer maxLength) {
        List<SquadWorkingPattern> squadWorkingPatterns = squadWorkingPatternRepository.findByCycleLengthBetween(minLength, maxLength);
        return squadWorkingPatternMapper.toDtoList(squadWorkingPatterns);
    }

    /**
     * Get active squad working pattern count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return squadWorkingPatternRepository.countByActiveTrue();
    }

    /**
     * Validate pattern format and content
     */
    @Transactional(readOnly = true)
    public List<String> validatePattern(String shiftPattern, Integer cycleLength) {
        return squadWorkingPatternMapper.validatePattern(shiftPattern, cycleLength);
    }

    /**
     * Validate squad working pattern data
     */
    private void validatePatternData(SquadWorkingPatternDTO dto) {
        if (!StringUtils.hasText(dto.getShiftPattern())) {
            throw new IllegalArgumentException("Shift pattern is required");
        }

        if (dto.getCycleLength() == null || dto.getCycleLength() < 1) {
            throw new IllegalArgumentException("Cycle length must be at least 1 day");
        }

        if (dto.getCycleLength() > 365) {
            throw new IllegalArgumentException("Cycle length cannot exceed 365 days");
        }

        // Validate pattern format
        List<String> patternErrors = squadWorkingPatternMapper.validatePattern(dto.getShiftPattern(), dto.getCycleLength());
        if (!patternErrors.isEmpty()) {
            throw new IllegalArgumentException("Invalid shift pattern: " + String.join(", ", patternErrors));
        }

        log.debug("Pattern validation passed for pattern: {}", dto.getShiftPattern());
    }
}
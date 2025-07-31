package com.rosteroptimization.service;

import com.rosteroptimization.dto.SquadDTO;
import com.rosteroptimization.entity.Squad;
import com.rosteroptimization.entity.SquadWorkingPattern;
import com.rosteroptimization.mapper.SquadMapper;
import com.rosteroptimization.repository.SquadRepository;
import com.rosteroptimization.repository.SquadWorkingPatternRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SquadService {

    private final SquadRepository squadRepository;
    private final SquadWorkingPatternRepository squadWorkingPatternRepository;
    private final SquadMapper squadMapper;

    /**
     * Create new squad
     */
    public SquadDTO create(SquadDTO dto) {
        log.info("Creating new squad: {}", dto.getName());

        // Validate squad data
        validateSquadData(dto);

        // Check if squad with same name already exists
        Optional<Squad> existing = squadRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Squad with name '" + dto.getName() + "' already exists");
        }

        Squad entity = squadMapper.toEntity(dto);
        Squad saved = squadRepository.save(entity);

        log.info("Squad created with ID: {}", saved.getId());
        return squadMapper.toDto(saved);
    }

    /**
     * Update existing squad
     */
    public SquadDTO update(Long id, SquadDTO dto) {
        log.info("Updating squad with ID: {}", id);

        // Validate squad data
        validateSquadData(dto);

        Squad existing = squadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + id));

        // Check if another squad with same name exists
        Optional<Squad> duplicateName = squadRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Squad with name '" + dto.getName() + "' already exists");
        }

        squadMapper.updateEntityFromDto(dto, existing);
        Squad updated = squadRepository.save(existing);

        log.info("Squad updated: {}", updated.getName());
        return squadMapper.toDto(updated);
    }

    /**
     * Soft delete squad
     */
    public void delete(Long id) {
        log.info("Soft deleting squad with ID: {}", id);

        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + id));

        // Check if squad has active staff
        long staffCount = squadRepository.countStaffInSquad(id);
        if (staffCount > 0) {
            throw new IllegalArgumentException("Cannot delete squad that has " + staffCount + " active staff member(s)");
        }

        squad.setActive(false);
        squadRepository.save(squad);

        log.info("Squad soft deleted: {}", squad.getName());
    }

    /**
     * Find squad by ID
     */
    @Transactional(readOnly = true)
    public SquadDTO findById(Long id) {
        Squad squad = squadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + id));

        return squadMapper.toDto(squad);
    }

    /**
     * Find all active squads
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findAll() {
        List<Squad> squads = squadRepository.findByActiveTrueOrderByNameAsc();
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find all squads (including inactive)
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findAllIncludingInactive() {
        List<Squad> squads = squadRepository.findAllByOrderByNameAsc();
        return squadMapper.toDtoList(squads);
    }

    /**
     * Search squads by name
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> searchByName(String name, boolean includeInactive) {
        List<Squad> squads = squadRepository.searchByNameContaining(name, !includeInactive);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find squads by working pattern
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findByWorkingPattern(Long squadWorkingPatternId) {
        List<Squad> squads = squadRepository.findBySquadWorkingPatternIdAndActiveTrue(squadWorkingPatternId);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find squads by start date
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findByStartDate(LocalDate startDate) {
        List<Squad> squads = squadRepository.findByStartDateAndActiveTrue(startDate);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find squads by start date range
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findByStartDateRange(LocalDate fromDate, LocalDate toDate) {
        validateDateRange(fromDate, toDate);
        List<Squad> squads = squadRepository.findByStartDateBetween(fromDate, toDate);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find new squads (started after specific date)
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findNewSquads(LocalDate afterDate) {
        List<Squad> squads = squadRepository.findByStartDateAfterAndActiveTrue(afterDate);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find squads with minimum staff count
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findSquadsWithMinimumStaff(int minStaffCount) {
        if (minStaffCount < 0) {
            throw new IllegalArgumentException("Minimum staff count cannot be negative");
        }
        List<Squad> squads = squadRepository.findSquadsWithMinimumStaff(minStaffCount);
        return squadMapper.toDtoList(squads);
    }

    /**
     * Find squads without staff
     */
    @Transactional(readOnly = true)
    public List<SquadDTO> findSquadsWithoutStaff() {
        List<Squad> squads = squadRepository.findSquadsWithoutStaff();
        return squadMapper.toDtoList(squads);
    }

    /**
     * Get active squad count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return squadRepository.countByActiveTrue();
    }

    /**
     * Get current cycle position for squad on specific date
     */
    @Transactional(readOnly = true)
    public String getCurrentCyclePosition(Long squadId, LocalDate date) {
        Squad squad = squadRepository.findById(squadId)
                .orElseThrow(() -> new IllegalArgumentException("Squad not found with ID: " + squadId));

        return squadMapper.getCurrentCyclePosition(squad, date);
    }

    /**
     * Get staff count in squad
     */
    @Transactional(readOnly = true)
    public long getStaffCount(Long squadId) {
        return squadRepository.countStaffInSquad(squadId);
    }

    /**
     * Validate squad data
     */
    private void validateSquadData(SquadDTO dto) {
        if (dto.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }

        if (dto.getSquadWorkingPatternId() == null) {
            throw new IllegalArgumentException("Squad working pattern is required");
        }

        // Validate start date is not in the future beyond reasonable limit
        LocalDate maxFutureDate = LocalDate.now().plusYears(1);
        if (dto.getStartDate().isAfter(maxFutureDate)) {
            throw new IllegalArgumentException("Start date cannot be more than 1 year in the future");
        }

        // Validate squad working pattern exists and is active
        SquadWorkingPattern pattern = squadWorkingPatternRepository.findById(dto.getSquadWorkingPatternId())
                .orElseThrow(() -> new IllegalArgumentException("Squad working pattern not found with ID: " + dto.getSquadWorkingPatternId()));

        if (!pattern.getActive()) {
            throw new IllegalArgumentException("Cannot assign squad to inactive working pattern");
        }

        log.debug("Squad validation passed for: {}", dto.getName());
    }

    /**
     * Validate date range
     */
    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Both from date and to date are required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date cannot be after to date");
        }

        log.debug("Date range validation passed: {} to {}", fromDate, toDate);
    }
}
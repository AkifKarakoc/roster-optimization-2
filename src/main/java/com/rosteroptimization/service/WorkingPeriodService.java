package com.rosteroptimization.service;

import com.rosteroptimization.dto.WorkingPeriodDTO;
import com.rosteroptimization.entity.WorkingPeriod;
import com.rosteroptimization.mapper.WorkingPeriodMapper;
import com.rosteroptimization.repository.WorkingPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkingPeriodService {

    private final WorkingPeriodRepository workingPeriodRepository;
    private final WorkingPeriodMapper workingPeriodMapper;

    /**
     * Create new working period
     */
    public WorkingPeriodDTO create(WorkingPeriodDTO dto) {
        log.info("Creating new working period: {}", dto.getName());

        // Validate time logic
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        // Check if working period with same name already exists
        Optional<WorkingPeriod> existing = workingPeriodRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Working period with name '" + dto.getName() + "' already exists");
        }

        WorkingPeriod entity = workingPeriodMapper.toEntity(dto);
        WorkingPeriod saved = workingPeriodRepository.save(entity);

        log.info("Working period created with ID: {}", saved.getId());
        return workingPeriodMapper.toDto(saved);
    }

    /**
     * Update existing working period
     */
    public WorkingPeriodDTO update(Long id, WorkingPeriodDTO dto) {
        log.info("Updating working period with ID: {}", id);

        // Validate time logic
        validateTimeRange(dto.getStartTime(), dto.getEndTime());

        WorkingPeriod existing = workingPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + id));

        // Check if another working period with same name exists
        Optional<WorkingPeriod> duplicateName = workingPeriodRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Working period with name '" + dto.getName() + "' already exists");
        }

        workingPeriodMapper.updateEntityFromDto(dto, existing);
        WorkingPeriod updated = workingPeriodRepository.save(existing);

        log.info("Working period updated: {}", updated.getName());
        return workingPeriodMapper.toDto(updated);
    }

    /**
     * Soft delete working period
     */
    public void delete(Long id) {
        log.info("Soft deleting working period with ID: {}", id);

        WorkingPeriod workingPeriod = workingPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + id));

        workingPeriod.setActive(false);
        workingPeriodRepository.save(workingPeriod);

        log.info("Working period soft deleted: {}", workingPeriod.getName());
    }

    /**
     * Find working period by ID
     */
    @Transactional(readOnly = true)
    public WorkingPeriodDTO findById(Long id) {
        WorkingPeriod workingPeriod = workingPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + id));

        return workingPeriodMapper.toDto(workingPeriod);
    }

    /**
     * Find all active working periods
     */
    @Transactional(readOnly = true)
    public List<WorkingPeriodDTO> findAll() {
        List<WorkingPeriod> workingPeriods = workingPeriodRepository.findByActiveTrueOrderByStartTimeAsc();
        return workingPeriodMapper.toDtoList(workingPeriods);
    }

    /**
     * Find all working periods (including inactive)
     */
    @Transactional(readOnly = true)
    public List<WorkingPeriodDTO> findAllIncludingInactive() {
        List<WorkingPeriod> workingPeriods = workingPeriodRepository.findAllByOrderByStartTimeAsc();
        return workingPeriodMapper.toDtoList(workingPeriods);
    }

    /**
     * Search working periods by name
     */
    @Transactional(readOnly = true)
    public List<WorkingPeriodDTO> searchByName(String name, boolean includeInactive) {
        List<WorkingPeriod> workingPeriods = workingPeriodRepository.searchByNameContaining(name, !includeInactive);
        return workingPeriodMapper.toDtoList(workingPeriods);
    }

    /**
     * Find working periods by time range
     */
    @Transactional(readOnly = true)
    public List<WorkingPeriodDTO> findByTimeRange(LocalTime startTime, LocalTime endTime) {
        validateTimeRange(startTime, endTime);
        List<WorkingPeriod> workingPeriods = workingPeriodRepository.findByTimeRange(startTime, endTime);
        return workingPeriodMapper.toDtoList(workingPeriods);
    }

    /**
     * Find overlapping working periods
     */
    @Transactional(readOnly = true)
    public List<WorkingPeriodDTO> findOverlappingPeriods(LocalTime startTime, LocalTime endTime) {
        validateTimeRange(startTime, endTime);
        List<WorkingPeriod> workingPeriods = workingPeriodRepository.findOverlappingPeriods(startTime, endTime);
        return workingPeriodMapper.toDtoList(workingPeriods);
    }

    /**
     * Get active working period count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return workingPeriodRepository.countByActiveTrue();
    }

    /**
     * Validate time range logic
     */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start time and end time are required");
        }

        // Allow crossing midnight but validate it's not the same time
        if (startTime.equals(endTime)) {
            throw new IllegalArgumentException("Start time and end time cannot be the same");
        }

        log.debug("Time range validation passed: {} - {}", startTime, endTime);
    }
}
package com.rosteroptimization.service;

import com.rosteroptimization.dto.ShiftDTO;
import com.rosteroptimization.entity.Shift;
import com.rosteroptimization.entity.WorkingPeriod;
import com.rosteroptimization.mapper.ShiftMapper;
import com.rosteroptimization.repository.ShiftRepository;
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
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final WorkingPeriodRepository workingPeriodRepository;
    private final ShiftMapper shiftMapper;

    /**
     * Create new shift
     */
    public ShiftDTO create(ShiftDTO dto) {
        log.info("Creating new shift: {}", dto.getName());

        // Validate time logic and working period
        validateShiftData(dto);

        // Check if shift with same name already exists
        Optional<Shift> existing = shiftRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Shift with name '" + dto.getName() + "' already exists");
        }

        Shift entity = shiftMapper.toEntity(dto);

        // Auto-detect night shift if not explicitly set
        if (dto.getIsNightShift() == null) {
            entity.setIsNightShift(isNightShift(dto.getStartTime(), dto.getEndTime()));
        }

        Shift saved = shiftRepository.save(entity);

        log.info("Shift created with ID: {}", saved.getId());
        return shiftMapper.toDto(saved);
    }

    /**
     * Update existing shift
     */
    public ShiftDTO update(Long id, ShiftDTO dto) {
        log.info("Updating shift with ID: {}", id);

        // Validate time logic and working period
        validateShiftData(dto);

        Shift existing = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with ID: " + id));

        // Check if another shift with same name exists
        Optional<Shift> duplicateName = shiftRepository.findByNameIgnoreCaseAndActiveTrue(dto.getName());
        if (duplicateName.isPresent() && !duplicateName.get().getId().equals(id)) {
            throw new IllegalArgumentException("Shift with name '" + dto.getName() + "' already exists");
        }

        shiftMapper.updateEntityFromDto(dto, existing);

        // Auto-detect night shift if not explicitly set
        if (dto.getIsNightShift() == null) {
            existing.setIsNightShift(isNightShift(dto.getStartTime(), dto.getEndTime()));
        }

        Shift updated = shiftRepository.save(existing);

        log.info("Shift updated: {}", updated.getName());
        return shiftMapper.toDto(updated);
    }

    /**
     * Soft delete shift
     */
    public void delete(Long id) {
        log.info("Soft deleting shift with ID: {}", id);

        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with ID: " + id));

        shift.setActive(false);
        shiftRepository.save(shift);

        log.info("Shift soft deleted: {}", shift.getName());
    }

    /**
     * Find shift by ID
     */
    @Transactional(readOnly = true)
    public ShiftDTO findById(Long id) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Shift not found with ID: " + id));

        return shiftMapper.toDto(shift);
    }

    /**
     * Find all active shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findAll() {
        List<Shift> shifts = shiftRepository.findByActiveTrueOrderByStartTimeAsc();
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find all shifts (including inactive)
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findAllIncludingInactive() {
        List<Shift> shifts = shiftRepository.findAllByOrderByStartTimeAsc();
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Search shifts by name
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> searchByName(String name, boolean includeInactive) {
        List<Shift> shifts = shiftRepository.searchByNameContaining(name, !includeInactive);
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find shifts by working period
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findByWorkingPeriod(Long workingPeriodId) {
        List<Shift> shifts = shiftRepository.findByWorkingPeriodIdAndActiveTrue(workingPeriodId);
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find night shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findNightShifts() {
        List<Shift> shifts = shiftRepository.findByIsNightShiftTrueAndActiveTrue();
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find day shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findDayShifts() {
        List<Shift> shifts = shiftRepository.findByIsNightShiftFalseAndActiveTrue();
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find shifts by time range
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findByTimeRange(LocalTime startTime, LocalTime endTime) {
        validateTimeRange(startTime, endTime);
        List<Shift> shifts = shiftRepository.findByTimeRange(startTime, endTime);
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Find overlapping shifts
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findOverlappingShifts(LocalTime startTime, LocalTime endTime) {
        validateTimeRange(startTime, endTime);
        List<Shift> shifts = shiftRepository.findOverlappingShifts(startTime, endTime);
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Get active shift count
     */
    @Transactional(readOnly = true)
    public long getActiveCount() {
        return shiftRepository.countByActiveTrue();
    }

    /**
     * Find shifts by IDs (for bulk operations)
     */
    @Transactional(readOnly = true)
    public List<ShiftDTO> findByIds(List<Long> ids) {
        List<Shift> shifts = shiftRepository.findByIdInAndActiveTrue(ids);
        return shiftMapper.toDtoList(shifts);
    }

    /**
     * Validate shift data
     */
    private void validateShiftData(ShiftDTO dto) {
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        validateWorkingPeriod(dto.getWorkingPeriodId());
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

    /**
     * Validate working period exists and is active
     */
    private void validateWorkingPeriod(Long workingPeriodId) {
        if (workingPeriodId == null) {
            throw new IllegalArgumentException("Working period is required");
        }

        WorkingPeriod workingPeriod = workingPeriodRepository.findById(workingPeriodId)
                .orElseThrow(() -> new IllegalArgumentException("Working period not found with ID: " + workingPeriodId));

        if (!workingPeriod.getActive()) {
            throw new IllegalArgumentException("Cannot assign shift to inactive working period");
        }

        log.debug("Working period validation passed: {}", workingPeriod.getName());
    }

    /**
     * Auto-detect if shift is night shift based on time
     */
    private Boolean isNightShift(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }

        // Consider night shift if:
        // 1. Crosses midnight (end time before start time)
        // 2. Starts after 22:00 or before 06:00
        // 3. Ends after 22:00 or before 06:00
        return endTime.isBefore(startTime) ||
                startTime.isAfter(LocalTime.of(22, 0)) ||
                startTime.isBefore(LocalTime.of(6, 0)) ||
                endTime.isAfter(LocalTime.of(22, 0)) ||
                endTime.isBefore(LocalTime.of(6, 0));
    }
}
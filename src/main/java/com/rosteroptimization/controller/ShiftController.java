package com.rosteroptimization.controller;

import com.rosteroptimization.dto.ShiftDTO;
import com.rosteroptimization.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/shifts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ShiftController {

    private final ShiftService shiftService;

    /**
     * Create new shift
     */
    @PostMapping
    public ResponseEntity<ShiftDTO> create(@Valid @RequestBody ShiftDTO dto) {
        log.info("POST /api/shifts - Creating shift: {}", dto.getName());

        try {
            ShiftDTO created = shiftService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating shift: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing shift
     */
    @PutMapping("/{id}")
    public ResponseEntity<ShiftDTO> update(@PathVariable Long id, @Valid @RequestBody ShiftDTO dto) {
        log.info("PUT /api/shifts/{} - Updating shift", id);

        try {
            ShiftDTO updated = shiftService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating shift: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete shift
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/shifts/{} - Deleting shift", id);

        try {
            shiftService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting shift: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get shift by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShiftDTO> findById(@PathVariable Long id) {
        log.info("GET /api/shifts/{} - Finding shift by ID", id);

        try {
            ShiftDTO shift = shiftService.findById(id);
            return ResponseEntity.ok(shift);
        } catch (IllegalArgumentException e) {
            log.error("Shift not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active shifts
     */
    @GetMapping
    public ResponseEntity<List<ShiftDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/shifts - Finding all shifts (includeInactive: {})", includeInactive);

        List<ShiftDTO> shifts = includeInactive
                ? shiftService.findAllIncludingInactive()
                : shiftService.findAll();

        return ResponseEntity.ok(shifts);
    }

    /**
     * Search shifts by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<ShiftDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/shifts/search - Searching shifts by name: {}", name);

        List<ShiftDTO> shifts = shiftService.searchByName(name, includeInactive);
        return ResponseEntity.ok(shifts);
    }

    /**
     * Find shifts by working period
     */
    @GetMapping("/working-period/{workingPeriodId}")
    public ResponseEntity<List<ShiftDTO>> findByWorkingPeriod(@PathVariable Long workingPeriodId) {
        log.info("GET /api/shifts/working-period/{} - Finding shifts by working period", workingPeriodId);

        List<ShiftDTO> shifts = shiftService.findByWorkingPeriod(workingPeriodId);
        return ResponseEntity.ok(shifts);
    }

    /**
     * Find night shifts
     */
    @GetMapping("/night")
    public ResponseEntity<List<ShiftDTO>> findNightShifts() {
        log.info("GET /api/shifts/night - Finding night shifts");

        List<ShiftDTO> shifts = shiftService.findNightShifts();
        return ResponseEntity.ok(shifts);
    }

    /**
     * Find day shifts
     */
    @GetMapping("/day")
    public ResponseEntity<List<ShiftDTO>> findDayShifts() {
        log.info("GET /api/shifts/day - Finding day shifts");

        List<ShiftDTO> shifts = shiftService.findDayShifts();
        return ResponseEntity.ok(shifts);
    }

    /**
     * Find shifts by time range
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<ShiftDTO>> findByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        log.info("GET /api/shifts/time-range - Finding shifts by time range: {} - {}", startTime, endTime);

        try {
            List<ShiftDTO> shifts = shiftService.findByTimeRange(startTime, endTime);
            return ResponseEntity.ok(shifts);
        } catch (IllegalArgumentException e) {
            log.error("Error finding shifts by time range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find overlapping shifts
     */
    @GetMapping("/overlapping")
    public ResponseEntity<List<ShiftDTO>> findOverlapping(
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        log.info("GET /api/shifts/overlapping - Finding overlapping shifts: {} - {}", startTime, endTime);

        try {
            List<ShiftDTO> shifts = shiftService.findOverlappingShifts(startTime, endTime);
            return ResponseEntity.ok(shifts);
        } catch (IllegalArgumentException e) {
            log.error("Error finding overlapping shifts: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active shift count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/shifts/count - Getting active shift count");

        long count = shiftService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get shifts by IDs (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<ShiftDTO>> findByIds(@RequestBody List<Long> ids) {
        log.info("POST /api/shifts/bulk - Finding shifts by IDs: {}", ids);

        List<ShiftDTO> shifts = shiftService.findByIds(ids);
        return ResponseEntity.ok(shifts);
    }
}
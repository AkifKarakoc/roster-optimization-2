package com.rosteroptimization.controller;

import com.rosteroptimization.dto.WorkingPeriodDTO;
import com.rosteroptimization.service.WorkingPeriodService;
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
@RequestMapping("/api/working-periods")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class WorkingPeriodController {

    private final WorkingPeriodService workingPeriodService;

    /**
     * Create new working period
     */
    @PostMapping
    public ResponseEntity<WorkingPeriodDTO> create(@Valid @RequestBody WorkingPeriodDTO dto) {
        log.info("POST /api/working-periods - Creating working period: {}", dto.getName());

        try {
            WorkingPeriodDTO created = workingPeriodService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating working period: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing working period
     */
    @PutMapping("/{id}")
    public ResponseEntity<WorkingPeriodDTO> update(@PathVariable Long id, @Valid @RequestBody WorkingPeriodDTO dto) {
        log.info("PUT /api/working-periods/{} - Updating working period", id);

        try {
            WorkingPeriodDTO updated = workingPeriodService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating working period: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete working period
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/working-periods/{} - Deleting working period", id);

        try {
            workingPeriodService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting working period: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get working period by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkingPeriodDTO> findById(@PathVariable Long id) {
        log.info("GET /api/working-periods/{} - Finding working period by ID", id);

        try {
            WorkingPeriodDTO workingPeriod = workingPeriodService.findById(id);
            return ResponseEntity.ok(workingPeriod);
        } catch (IllegalArgumentException e) {
            log.error("Working period not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active working periods
     */
    @GetMapping
    public ResponseEntity<List<WorkingPeriodDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/working-periods - Finding all working periods (includeInactive: {})", includeInactive);

        List<WorkingPeriodDTO> workingPeriods = includeInactive
                ? workingPeriodService.findAllIncludingInactive()
                : workingPeriodService.findAll();

        return ResponseEntity.ok(workingPeriods);
    }

    /**
     * Search working periods by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<WorkingPeriodDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/working-periods/search - Searching working periods by name: {}", name);

        List<WorkingPeriodDTO> workingPeriods = workingPeriodService.searchByName(name, includeInactive);
        return ResponseEntity.ok(workingPeriods);
    }

    /**
     * Find working periods by time range
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<WorkingPeriodDTO>> findByTimeRange(
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        log.info("GET /api/working-periods/time-range - Finding working periods by time range: {} - {}", startTime, endTime);

        try {
            List<WorkingPeriodDTO> workingPeriods = workingPeriodService.findByTimeRange(startTime, endTime);
            return ResponseEntity.ok(workingPeriods);
        } catch (IllegalArgumentException e) {
            log.error("Error finding working periods by time range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find overlapping working periods
     */
    @GetMapping("/overlapping")
    public ResponseEntity<List<WorkingPeriodDTO>> findOverlapping(
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime) {
        log.info("GET /api/working-periods/overlapping - Finding overlapping working periods: {} - {}", startTime, endTime);

        try {
            List<WorkingPeriodDTO> workingPeriods = workingPeriodService.findOverlappingPeriods(startTime, endTime);
            return ResponseEntity.ok(workingPeriods);
        } catch (IllegalArgumentException e) {
            log.error("Error finding overlapping working periods: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active working period count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/working-periods/count - Getting active working period count");

        long count = workingPeriodService.getActiveCount();
        return ResponseEntity.ok(count);
    }
}
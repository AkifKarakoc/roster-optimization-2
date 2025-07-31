package com.rosteroptimization.controller;

import com.rosteroptimization.dto.SquadDTO;
import com.rosteroptimization.service.SquadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/squads")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SquadController {

    private final SquadService squadService;

    /**
     * Create new squad
     */
    @PostMapping
    public ResponseEntity<SquadDTO> create(@Valid @RequestBody SquadDTO dto) {
        log.info("POST /api/squads - Creating squad: {}", dto.getName());

        try {
            SquadDTO created = squadService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating squad: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing squad
     */
    @PutMapping("/{id}")
    public ResponseEntity<SquadDTO> update(@PathVariable Long id, @Valid @RequestBody SquadDTO dto) {
        log.info("PUT /api/squads/{} - Updating squad", id);

        try {
            SquadDTO updated = squadService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating squad: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete squad
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/squads/{} - Deleting squad", id);

        try {
            squadService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting squad: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get squad by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SquadDTO> findById(@PathVariable Long id) {
        log.info("GET /api/squads/{} - Finding squad by ID", id);

        try {
            SquadDTO squad = squadService.findById(id);
            return ResponseEntity.ok(squad);
        } catch (IllegalArgumentException e) {
            log.error("Squad not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active squads
     */
    @GetMapping
    public ResponseEntity<List<SquadDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/squads - Finding all squads (includeInactive: {})", includeInactive);

        List<SquadDTO> squads = includeInactive
                ? squadService.findAllIncludingInactive()
                : squadService.findAll();

        return ResponseEntity.ok(squads);
    }

    /**
     * Search squads by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<SquadDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/squads/search - Searching squads by name: {}", name);

        List<SquadDTO> squads = squadService.searchByName(name, includeInactive);
        return ResponseEntity.ok(squads);
    }

    /**
     * Find squads by working pattern
     */
    @GetMapping("/working-pattern/{patternId}")
    public ResponseEntity<List<SquadDTO>> findByWorkingPattern(@PathVariable Long patternId) {
        log.info("GET /api/squads/working-pattern/{} - Finding squads by working pattern", patternId);

        List<SquadDTO> squads = squadService.findByWorkingPattern(patternId);
        return ResponseEntity.ok(squads);
    }

    /**
     * Find squads by start date
     */
    @GetMapping("/start-date/{startDate}")
    public ResponseEntity<List<SquadDTO>> findByStartDate(
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate) {
        log.info("GET /api/squads/start-date/{} - Finding squads by start date", startDate);

        List<SquadDTO> squads = squadService.findByStartDate(startDate);
        return ResponseEntity.ok(squads);
    }

    /**
     * Find squads by start date range
     */
    @GetMapping("/start-date-range")
    public ResponseEntity<List<SquadDTO>> findByStartDateRange(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate fromDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate toDate) {
        log.info("GET /api/squads/start-date-range - Finding squads by start date range: {} to {}", fromDate, toDate);

        try {
            List<SquadDTO> squads = squadService.findByStartDateRange(fromDate, toDate);
            return ResponseEntity.ok(squads);
        } catch (IllegalArgumentException e) {
            log.error("Error finding squads by start date range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find new squads (started after specific date)
     */
    @GetMapping("/new")
    public ResponseEntity<List<SquadDTO>> findNewSquads(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate afterDate) {

        // Default to 30 days ago if no date provided
        LocalDate searchDate = afterDate != null ? afterDate : LocalDate.now().minusDays(30);
        log.info("GET /api/squads/new - Finding new squads started after: {}", searchDate);

        List<SquadDTO> squads = squadService.findNewSquads(searchDate);
        return ResponseEntity.ok(squads);
    }

    /**
     * Find squads with minimum staff count
     */
    @GetMapping("/with-staff")
    public ResponseEntity<List<SquadDTO>> findSquadsWithMinimumStaff(
            @RequestParam(defaultValue = "1") int minStaffCount) {
        log.info("GET /api/squads/with-staff - Finding squads with minimum {} staff", minStaffCount);

        try {
            List<SquadDTO> squads = squadService.findSquadsWithMinimumStaff(minStaffCount);
            return ResponseEntity.ok(squads);
        } catch (IllegalArgumentException e) {
            log.error("Error finding squads with minimum staff: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find squads without staff
     */
    @GetMapping("/without-staff")
    public ResponseEntity<List<SquadDTO>> findSquadsWithoutStaff() {
        log.info("GET /api/squads/without-staff - Finding squads without staff");

        List<SquadDTO> squads = squadService.findSquadsWithoutStaff();
        return ResponseEntity.ok(squads);
    }

    /**
     * Get current cycle position for squad
     */
    @GetMapping("/{id}/cycle-position")
    public ResponseEntity<CyclePositionResponse> getCurrentCyclePosition(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        // Default to today if no date provided
        LocalDate searchDate = date != null ? date : LocalDate.now();
        log.info("GET /api/squads/{}/cycle-position - Getting cycle position for date: {}", id, searchDate);

        try {
            String position = squadService.getCurrentCyclePosition(id, searchDate);
            return ResponseEntity.ok(new CyclePositionResponse(searchDate, position));
        } catch (IllegalArgumentException e) {
            log.error("Error getting cycle position: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get staff count in squad
     */
    @GetMapping("/{id}/staff-count")
    public ResponseEntity<Long> getStaffCount(@PathVariable Long id) {
        log.info("GET /api/squads/{}/staff-count - Getting staff count for squad", id);

        try {
            long staffCount = squadService.getStaffCount(id);
            return ResponseEntity.ok(staffCount);
        } catch (IllegalArgumentException e) {
            log.error("Error getting staff count: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get active squad count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/squads/count - Getting active squad count");

        long count = squadService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    // Inner class for cycle position response
    public static class CyclePositionResponse {
        private LocalDate date;
        private String position;

        public CyclePositionResponse(LocalDate date, String position) {
            this.date = date;
            this.position = position;
        }

        // Getters and setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
    }
}
package com.rosteroptimization.controller;

import com.rosteroptimization.dto.ConstraintDTO;
import com.rosteroptimization.entity.Constraint;
import com.rosteroptimization.service.ConstraintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/constraints")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ConstraintController {

    private final ConstraintService constraintService;

    /**
     * Create new constraint
     */
    @PostMapping
    public ResponseEntity<ConstraintDTO> create(@Valid @RequestBody ConstraintDTO dto) {
        log.info("POST /api/constraints - Creating constraint: {}", dto.getName());

        try {
            ConstraintDTO created = constraintService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating constraint: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing constraint
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConstraintDTO> update(@PathVariable Long id, @Valid @RequestBody ConstraintDTO dto) {
        log.info("PUT /api/constraints/{} - Updating constraint", id);

        try {
            ConstraintDTO updated = constraintService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating constraint: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete constraint
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/constraints/{} - Deleting constraint", id);

        try {
            constraintService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting constraint: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get constraint by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConstraintDTO> findById(@PathVariable Long id) {
        log.info("GET /api/constraints/{} - Finding constraint by ID", id);

        try {
            ConstraintDTO constraint = constraintService.findById(id);
            return ResponseEntity.ok(constraint);
        } catch (IllegalArgumentException e) {
            log.error("Constraint not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active constraints
     */
    @GetMapping
    public ResponseEntity<List<ConstraintDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/constraints - Finding all constraints (includeInactive: {})", includeInactive);

        List<ConstraintDTO> constraints = includeInactive
                ? constraintService.findAllIncludingInactive()
                : constraintService.findAll();

        return ResponseEntity.ok(constraints);
    }

    /**
     * Search constraints by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<ConstraintDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/constraints/search - Searching constraints by name: {}", name);

        List<ConstraintDTO> constraints = constraintService.searchByName(name, includeInactive);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Get constraint by name
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<ConstraintDTO> findByName(@PathVariable String name) {
        log.info("GET /api/constraints/name/{} - Finding constraint by name", name);

        try {
            ConstraintDTO constraint = constraintService.findByName(name);
            return ResponseEntity.ok(constraint);
        } catch (IllegalArgumentException e) {
            log.error("Constraint not found by name: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Find constraints by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<ConstraintDTO>> findByType(@PathVariable Constraint.ConstraintType type) {
        log.info("GET /api/constraints/type/{} - Finding constraints by type", type);

        List<ConstraintDTO> constraints = constraintService.findByType(type);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Find HARD constraints
     */
    @GetMapping("/hard")
    public ResponseEntity<List<ConstraintDTO>> findHardConstraints() {
        log.info("GET /api/constraints/hard - Finding HARD constraints");

        List<ConstraintDTO> constraints = constraintService.findHardConstraints();
        return ResponseEntity.ok(constraints);
    }

    /**
     * Find SOFT constraints
     */
    @GetMapping("/soft")
    public ResponseEntity<List<ConstraintDTO>> findSoftConstraints() {
        log.info("GET /api/constraints/soft - Finding SOFT constraints");

        List<ConstraintDTO> constraints = constraintService.findSoftConstraints();
        return ResponseEntity.ok(constraints);
    }

    /**
     * Search constraints by description
     */
    @GetMapping("/search-description")
    public ResponseEntity<List<ConstraintDTO>> searchByDescription(@RequestParam String description) {
        log.info("GET /api/constraints/search-description - Searching constraints by description: {}", description);

        List<ConstraintDTO> constraints = constraintService.searchByDescription(description);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Find constraints with overrides
     */
    @GetMapping("/with-overrides")
    public ResponseEntity<List<ConstraintDTO>> findConstraintsWithOverrides() {
        log.info("GET /api/constraints/with-overrides - Finding constraints with overrides");

        List<ConstraintDTO> constraints = constraintService.findConstraintsWithOverrides();
        return ResponseEntity.ok(constraints);
    }

    /**
     * Find constraints without overrides
     */
    @GetMapping("/without-overrides")
    public ResponseEntity<List<ConstraintDTO>> findConstraintsWithoutOverrides() {
        log.info("GET /api/constraints/without-overrides - Finding constraints without overrides");

        List<ConstraintDTO> constraints = constraintService.findConstraintsWithoutOverrides();
        return ResponseEntity.ok(constraints);
    }

    /**
     * Get active constraint count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/constraints/count - Getting active constraint count");

        long count = constraintService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get constraint count by type
     */
    @GetMapping("/count/{type}")
    public ResponseEntity<Long> getCountByType(@PathVariable Constraint.ConstraintType type) {
        log.info("GET /api/constraints/count/{} - Getting constraint count by type", type);

        long count = constraintService.getCountByType(type);
        return ResponseEntity.ok(count);
    }

    /**
     * Get count of constraints with overrides
     */
    @GetMapping("/count/with-overrides")
    public ResponseEntity<Long> getConstraintsWithOverridesCount() {
        log.info("GET /api/constraints/count/with-overrides - Getting constraints with overrides count");

        long count = constraintService.getConstraintsWithOverridesCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get effective constraint value for staff
     */
    @GetMapping("/effective-value")
    public ResponseEntity<String> getEffectiveValueForStaff(
            @RequestParam String constraintName,
            @RequestParam Long staffId) {
        log.info("GET /api/constraints/effective-value - Getting effective value for constraint: {} and staff: {}", constraintName, staffId);

        try {
            String effectiveValue = constraintService.getEffectiveValueForStaff(constraintName, staffId);
            return ResponseEntity.ok(effectiveValue);
        } catch (IllegalArgumentException e) {
            log.error("Error getting effective value: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get constraints by IDs (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<ConstraintDTO>> findByIds(@RequestBody List<Long> ids) {
        log.info("POST /api/constraints/bulk - Finding constraints by IDs: {}", ids);

        List<ConstraintDTO> constraints = constraintService.findByIds(ids);
        return ResponseEntity.ok(constraints);
    }

    /**
     * Bulk update constraints
     */
    @PutMapping("/bulk")
    public ResponseEntity<List<ConstraintDTO>> bulkUpdate(@Valid @RequestBody List<ConstraintDTO> constraints) {
        log.info("PUT /api/constraints/bulk - Bulk updating {} constraints", constraints.size());

        try {
            List<ConstraintDTO> updated = constraintService.bulkUpdate(constraints);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error in bulk update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get constraint statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ConstraintStatistics> getStatistics() {
        log.info("GET /api/constraints/statistics - Getting constraint statistics");

        long totalCount = constraintService.getActiveCount();
        long hardCount = constraintService.getCountByType(Constraint.ConstraintType.HARD);
        long softCount = constraintService.getCountByType(Constraint.ConstraintType.SOFT);
        long withOverridesCount = constraintService.getConstraintsWithOverridesCount();

        ConstraintStatistics stats = new ConstraintStatistics(totalCount, hardCount, softCount, withOverridesCount);
        return ResponseEntity.ok(stats);
    }

    // Inner class for statistics response
    public static class ConstraintStatistics {
        private long totalConstraints;
        private long hardConstraints;
        private long softConstraints;
        private long constraintsWithOverrides;
        private double hardPercentage;
        private double softPercentage;
        private double overridePercentage;

        public ConstraintStatistics(long totalConstraints, long hardConstraints, long softConstraints, long constraintsWithOverrides) {
            this.totalConstraints = totalConstraints;
            this.hardConstraints = hardConstraints;
            this.softConstraints = softConstraints;
            this.constraintsWithOverrides = constraintsWithOverrides;

            if (totalConstraints > 0) {
                this.hardPercentage = Math.round((double) hardConstraints / totalConstraints * 10000.0) / 100.0;
                this.softPercentage = Math.round((double) softConstraints / totalConstraints * 10000.0) / 100.0;
                this.overridePercentage = Math.round((double) constraintsWithOverrides / totalConstraints * 10000.0) / 100.0;
            }
        }

        // Getters and setters
        public long getTotalConstraints() { return totalConstraints; }
        public void setTotalConstraints(long totalConstraints) { this.totalConstraints = totalConstraints; }
        public long getHardConstraints() { return hardConstraints; }
        public void setHardConstraints(long hardConstraints) { this.hardConstraints = hardConstraints; }
        public long getSoftConstraints() { return softConstraints; }
        public void setSoftConstraints(long softConstraints) { this.softConstraints = softConstraints; }
        public long getConstraintsWithOverrides() { return constraintsWithOverrides; }
        public void setConstraintsWithOverrides(long constraintsWithOverrides) { this.constraintsWithOverrides = constraintsWithOverrides; }
        public double getHardPercentage() { return hardPercentage; }
        public void setHardPercentage(double hardPercentage) { this.hardPercentage = hardPercentage; }
        public double getSoftPercentage() { return softPercentage; }
        public void setSoftPercentage(double softPercentage) { this.softPercentage = softPercentage; }
        public double getOverridePercentage() { return overridePercentage; }
        public void setOverridePercentage(double overridePercentage) { this.overridePercentage = overridePercentage; }
    }
}
package com.rosteroptimization.controller;

import com.rosteroptimization.dto.ConstraintOverrideDTO;
import com.rosteroptimization.service.ConstraintOverrideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/constraint-overrides")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class ConstraintOverrideController {

    private final ConstraintOverrideService constraintOverrideService;

    /**
     * Create new constraint override
     */
    @PostMapping
    public ResponseEntity<ConstraintOverrideDTO> create(@Valid @RequestBody ConstraintOverrideDTO dto) {
        log.info("POST /api/constraint-overrides - Creating constraint override for staff: {} and constraint: {}",
                dto.getStaffId(), dto.getConstraintId());

        try {
            ConstraintOverrideDTO created = constraintOverrideService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating constraint override: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing constraint override
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConstraintOverrideDTO> update(@PathVariable Long id, @Valid @RequestBody ConstraintOverrideDTO dto) {
        log.info("PUT /api/constraint-overrides/{} - Updating constraint override", id);

        try {
            ConstraintOverrideDTO updated = constraintOverrideService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating constraint override: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete constraint override
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/constraint-overrides/{} - Deleting constraint override", id);

        try {
            constraintOverrideService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting constraint override: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete constraint override by staff and constraint
     */
    @DeleteMapping("/staff/{staffId}/constraint/{constraintId}")
    public ResponseEntity<Void> deleteByStaffAndConstraint(@PathVariable Long staffId, @PathVariable Long constraintId) {
        log.info("DELETE /api/constraint-overrides/staff/{}/constraint/{} - Deleting constraint override", staffId, constraintId);

        try {
            constraintOverrideService.deleteByStaffAndConstraint(staffId, constraintId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting constraint override: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get constraint override by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConstraintOverrideDTO> findById(@PathVariable Long id) {
        log.info("GET /api/constraint-overrides/{} - Finding constraint override by ID", id);

        try {
            ConstraintOverrideDTO constraintOverride = constraintOverrideService.findById(id);
            return ResponseEntity.ok(constraintOverride);
        } catch (IllegalArgumentException e) {
            log.error("Constraint override not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all constraint overrides
     */
    @GetMapping
    public ResponseEntity<List<ConstraintOverrideDTO>> findAll() {
        log.info("GET /api/constraint-overrides - Finding all constraint overrides");

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findAll();
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find constraint overrides by staff ID
     */
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findByStaffId(@PathVariable Long staffId) {
        log.info("GET /api/constraint-overrides/staff/{} - Finding constraint overrides by staff ID", staffId);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findByStaffId(staffId);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find constraint overrides by constraint ID
     */
    @GetMapping("/constraint/{constraintId}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findByConstraintId(@PathVariable Long constraintId) {
        log.info("GET /api/constraint-overrides/constraint/{} - Finding constraint overrides by constraint ID", constraintId);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findByConstraintId(constraintId);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Get constraint override by staff and constraint
     */
    @GetMapping("/staff/{staffId}/constraint/{constraintId}")
    public ResponseEntity<ConstraintOverrideDTO> findByStaffAndConstraint(@PathVariable Long staffId, @PathVariable Long constraintId) {
        log.info("GET /api/constraint-overrides/staff/{}/constraint/{} - Finding constraint override by staff and constraint", staffId, constraintId);

        try {
            ConstraintOverrideDTO constraintOverride = constraintOverrideService.findByStaffAndConstraint(staffId, constraintId);
            return ResponseEntity.ok(constraintOverride);
        } catch (IllegalArgumentException e) {
            log.error("Constraint override not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get optional constraint override by staff and constraint
     */
    @GetMapping("/staff/{staffId}/constraint/{constraintId}/optional")
    public ResponseEntity<ConstraintOverrideDTO> findOptionalByStaffAndConstraint(@PathVariable Long staffId, @PathVariable Long constraintId) {
        log.info("GET /api/constraint-overrides/staff/{}/constraint/{}/optional - Finding optional constraint override", staffId, constraintId);

        Optional<ConstraintOverrideDTO> constraintOverride = constraintOverrideService.findOptionalByStaffAndConstraint(staffId, constraintId);
        return constraintOverride.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find constraint overrides by staff registration code
     */
    @GetMapping("/staff/registration/{registrationCode}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findByStaffRegistrationCode(@PathVariable String registrationCode) {
        log.info("GET /api/constraint-overrides/staff/registration/{} - Finding constraint overrides by staff registration code", registrationCode);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findByStaffRegistrationCode(registrationCode);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find constraint overrides by constraint name
     */
    @GetMapping("/constraint/name/{constraintName}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findByConstraintName(@PathVariable String constraintName) {
        log.info("GET /api/constraint-overrides/constraint/name/{} - Finding constraint overrides by constraint name", constraintName);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findByConstraintName(constraintName);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find constraint overrides by department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/constraint-overrides/department/{} - Finding constraint overrides by department", departmentId);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findByDepartment(departmentId);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find constraint overrides by squad
     */
    @GetMapping("/squad/{squadId}")
    public ResponseEntity<List<ConstraintOverrideDTO>> findBySquad(@PathVariable Long squadId) {
        log.info("GET /api/constraint-overrides/squad/{} - Finding constraint overrides by squad", squadId);

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findBySquad(squadId);
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find HARD constraint overrides
     */
    @GetMapping("/hard")
    public ResponseEntity<List<ConstraintOverrideDTO>> findHardConstraintOverrides() {
        log.info("GET /api/constraint-overrides/hard - Finding HARD constraint overrides");

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findHardConstraintOverrides();
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Find SOFT constraint overrides
     */
    @GetMapping("/soft")
    public ResponseEntity<List<ConstraintOverrideDTO>> findSoftConstraintOverrides() {
        log.info("GET /api/constraint-overrides/soft - Finding SOFT constraint overrides");

        List<ConstraintOverrideDTO> constraintOverrides = constraintOverrideService.findSoftConstraintOverrides();
        return ResponseEntity.ok(constraintOverrides);
    }

    /**
     * Check if constraint override exists
     */
    @GetMapping("/staff/{staffId}/constraint/{constraintId}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable Long staffId, @PathVariable Long constraintId) {
        log.info("GET /api/constraint-overrides/staff/{}/constraint/{}/exists - Checking if constraint override exists", staffId, constraintId);

        boolean exists = constraintOverrideService.exists(staffId, constraintId);
        return ResponseEntity.ok(exists);
    }

    /**
     * Get effective constraint value for staff
     */
    @GetMapping("/effective-value")
    public ResponseEntity<String> getEffectiveValue(
            @RequestParam Long staffId,
            @RequestParam String constraintName) {
        log.info("GET /api/constraint-overrides/effective-value - Getting effective value for staff: {} and constraint: {}", staffId, constraintName);

        try {
            String effectiveValue = constraintOverrideService.getEffectiveValue(staffId, constraintName);
            return ResponseEntity.ok(effectiveValue);
        } catch (IllegalArgumentException e) {
            log.error("Error getting effective value: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get total constraint override count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        log.info("GET /api/constraint-overrides/count - Getting total constraint override count");

        long count = constraintOverrideService.getTotalCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get constraint override count by staff
     */
    @GetMapping("/count/staff/{staffId}")
    public ResponseEntity<Long> getCountByStaff(@PathVariable Long staffId) {
        log.info("GET /api/constraint-overrides/count/staff/{} - Getting constraint override count by staff", staffId);

        long count = constraintOverrideService.getCountByStaff(staffId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get constraint override count by constraint
     */
    @GetMapping("/count/constraint/{constraintId}")
    public ResponseEntity<Long> getCountByConstraint(@PathVariable Long constraintId) {
        log.info("GET /api/constraint-overrides/count/constraint/{} - Getting constraint override count by constraint", constraintId);

        long count = constraintOverrideService.getCountByConstraint(constraintId);
        return ResponseEntity.ok(count);
    }

    /**
     * Bulk create or update constraint overrides for staff
     */
    @PostMapping("/staff/{staffId}/bulk")
    public ResponseEntity<List<ConstraintOverrideDTO>> bulkCreateOrUpdateForStaff(
            @PathVariable Long staffId,
            @Valid @RequestBody List<ConstraintOverrideDTO> overrides) {
        log.info("POST /api/constraint-overrides/staff/{}/bulk - Bulk creating/updating {} constraint overrides", staffId, overrides.size());

        try {
            List<ConstraintOverrideDTO> results = constraintOverrideService.bulkCreateOrUpdateForStaff(staffId, overrides);
            return ResponseEntity.ok(results);
        } catch (IllegalArgumentException e) {
            log.error("Error in bulk operation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
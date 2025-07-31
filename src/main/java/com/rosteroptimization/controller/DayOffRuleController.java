package com.rosteroptimization.controller;

import com.rosteroptimization.dto.DayOffRuleDTO;
import com.rosteroptimization.service.DayOffRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/day-off-rules")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class DayOffRuleController {

    private final DayOffRuleService dayOffRuleService;

    /**
     * Create new day off rule
     */
    @PostMapping
    public ResponseEntity<DayOffRuleDTO> create(@Valid @RequestBody DayOffRuleDTO dto) {
        log.info("POST /api/day-off-rules - Creating day off rule for staff ID: {}", dto.getStaffId());

        try {
            DayOffRuleDTO created = dayOffRuleService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating day off rule: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing day off rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<DayOffRuleDTO> update(@PathVariable Long id, @Valid @RequestBody DayOffRuleDTO dto) {
        log.info("PUT /api/day-off-rules/{} - Updating day off rule", id);

        try {
            DayOffRuleDTO updated = dayOffRuleService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating day off rule: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete day off rule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/day-off-rules/{} - Deleting day off rule", id);

        try {
            dayOffRuleService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting day off rule: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get day off rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DayOffRuleDTO> findById(@PathVariable Long id) {
        log.info("GET /api/day-off-rules/{} - Finding day off rule by ID", id);

        try {
            DayOffRuleDTO dayOffRule = dayOffRuleService.findById(id);
            return ResponseEntity.ok(dayOffRule);
        } catch (IllegalArgumentException e) {
            log.error("Day off rule not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all day off rules
     */
    @GetMapping
    public ResponseEntity<List<DayOffRuleDTO>> findAll() {
        log.info("GET /api/day-off-rules - Finding all day off rules");

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findAll();
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Get day off rule by staff ID
     */
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<DayOffRuleDTO> findByStaffId(@PathVariable Long staffId) {
        log.info("GET /api/day-off-rules/staff/{} - Finding day off rule by staff ID", staffId);

        try {
            DayOffRuleDTO dayOffRule = dayOffRuleService.findByStaffId(staffId);
            return ResponseEntity.ok(dayOffRule);
        } catch (IllegalArgumentException e) {
            log.error("Day off rule not found for staff: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get optional day off rule by staff ID
     */
    @GetMapping("/staff/{staffId}/optional")
    public ResponseEntity<DayOffRuleDTO> findOptionalByStaffId(@PathVariable Long staffId) {
        log.info("GET /api/day-off-rules/staff/{}/optional - Finding optional day off rule by staff ID", staffId);

        Optional<DayOffRuleDTO> dayOffRule = dayOffRuleService.findOptionalByStaffId(staffId);
        return dayOffRule.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get day off rule by staff registration code
     */
    @GetMapping("/staff/registration/{registrationCode}")
    public ResponseEntity<DayOffRuleDTO> findByStaffRegistrationCode(@PathVariable String registrationCode) {
        log.info("GET /api/day-off-rules/staff/registration/{} - Finding day off rule by registration code", registrationCode);

        try {
            DayOffRuleDTO dayOffRule = dayOffRuleService.findByStaffRegistrationCode(registrationCode);
            return ResponseEntity.ok(dayOffRule);
        } catch (IllegalArgumentException e) {
            log.error("Day off rule not found for registration code: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Find day off rules by working days
     */
    @GetMapping("/working-days/{workingDays}")
    public ResponseEntity<List<DayOffRuleDTO>> findByWorkingDays(@PathVariable Integer workingDays) {
        log.info("GET /api/day-off-rules/working-days/{} - Finding day off rules by working days", workingDays);

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findByWorkingDays(workingDays);
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Find day off rules by off days
     */
    @GetMapping("/off-days/{offDays}")
    public ResponseEntity<List<DayOffRuleDTO>> findByOffDays(@PathVariable Integer offDays) {
        log.info("GET /api/day-off-rules/off-days/{} - Finding day off rules by off days", offDays);

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findByOffDays(offDays);
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Find day off rules with fixed off days
     */
    @GetMapping("/fixed")
    public ResponseEntity<List<DayOffRuleDTO>> findRulesWithFixedOffDays() {
        log.info("GET /api/day-off-rules/fixed - Finding day off rules with fixed off days");

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findRulesWithFixedOffDays();
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Find flexible day off rules (without fixed off days)
     */
    @GetMapping("/flexible")
    public ResponseEntity<List<DayOffRuleDTO>> findFlexibleRules() {
        log.info("GET /api/day-off-rules/flexible - Finding flexible day off rules");

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findFlexibleRules();
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Find day off rules by specific fixed off day
     */
    @GetMapping("/fixed-day/{dayOfWeek}")
    public ResponseEntity<List<DayOffRuleDTO>> findByFixedOffDay(@PathVariable String dayOfWeek) {
        log.info("GET /api/day-off-rules/fixed-day/{} - Finding day off rules by fixed off day", dayOfWeek);

        try {
            List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findByFixedOffDay(dayOfWeek);
            return ResponseEntity.ok(dayOffRules);
        } catch (IllegalArgumentException e) {
            log.error("Error finding day off rules by fixed off day: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Find day off rules by department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<DayOffRuleDTO>> findByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/day-off-rules/department/{} - Finding day off rules by department", departmentId);

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findByDepartment(departmentId);
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Find day off rules by squad
     */
    @GetMapping("/squad/{squadId}")
    public ResponseEntity<List<DayOffRuleDTO>> findBySquad(@PathVariable Long squadId) {
        log.info("GET /api/day-off-rules/squad/{} - Finding day off rules by squad", squadId);

        List<DayOffRuleDTO> dayOffRules = dayOffRuleService.findBySquad(squadId);
        return ResponseEntity.ok(dayOffRules);
    }

    /**
     * Check if staff has day off rule
     */
    @GetMapping("/staff/{staffId}/exists")
    public ResponseEntity<Boolean> hasRule(@PathVariable Long staffId) {
        log.info("GET /api/day-off-rules/staff/{}/exists - Checking if staff has day off rule", staffId);

        boolean hasRule = dayOffRuleService.hasRule(staffId);
        return ResponseEntity.ok(hasRule);
    }

    /**
     * Get total day off rule count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getTotalCount() {
        log.info("GET /api/day-off-rules/count - Getting total day off rule count");

        long count = dayOffRuleService.getTotalCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get count of rules with fixed off days
     */
    @GetMapping("/count/fixed")
    public ResponseEntity<Long> getFixedRulesCount() {
        log.info("GET /api/day-off-rules/count/fixed - Getting fixed rules count");

        long count = dayOffRuleService.getFixedRulesCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get count of flexible rules
     */
    @GetMapping("/count/flexible")
    public ResponseEntity<Long> getFlexibleRulesCount() {
        log.info("GET /api/day-off-rules/count/flexible - Getting flexible rules count");

        long count = dayOffRuleService.getFlexibleRulesCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get rule statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<RuleStatistics> getStatistics() {
        log.info("GET /api/day-off-rules/statistics - Getting rule statistics");

        long totalCount = dayOffRuleService.getTotalCount();
        long fixedCount = dayOffRuleService.getFixedRulesCount();
        long flexibleCount = dayOffRuleService.getFlexibleRulesCount();

        RuleStatistics stats = new RuleStatistics(totalCount, fixedCount, flexibleCount);
        return ResponseEntity.ok(stats);
    }

    // Inner class for statistics response
    public static class RuleStatistics {
        private long totalRules;
        private long fixedRules;
        private long flexibleRules;
        private double fixedPercentage;
        private double flexiblePercentage;

        public RuleStatistics(long totalRules, long fixedRules, long flexibleRules) {
            this.totalRules = totalRules;
            this.fixedRules = fixedRules;
            this.flexibleRules = flexibleRules;

            if (totalRules > 0) {
                this.fixedPercentage = Math.round((double) fixedRules / totalRules * 10000.0) / 100.0;
                this.flexiblePercentage = Math.round((double) flexibleRules / totalRules * 10000.0) / 100.0;
            }
        }

        // Getters and setters
        public long getTotalRules() { return totalRules; }
        public void setTotalRules(long totalRules) { this.totalRules = totalRules; }
        public long getFixedRules() { return fixedRules; }
        public void setFixedRules(long fixedRules) { this.fixedRules = fixedRules; }
        public long getFlexibleRules() { return flexibleRules; }
        public void setFlexibleRules(long flexibleRules) { this.flexibleRules = flexibleRules; }
        public double getFixedPercentage() { return fixedPercentage; }
        public void setFixedPercentage(double fixedPercentage) { this.fixedPercentage = fixedPercentage; }
        public double getFlexiblePercentage() { return flexiblePercentage; }
        public void setFlexiblePercentage(double flexiblePercentage) { this.flexiblePercentage = flexiblePercentage; }
    }
}
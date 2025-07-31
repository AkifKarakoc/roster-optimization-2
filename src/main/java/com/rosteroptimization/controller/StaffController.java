package com.rosteroptimization.controller;

import com.rosteroptimization.dto.StaffDTO;
import com.rosteroptimization.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class StaffController {

    private final StaffService staffService;

    /**
     * Create new staff
     */
    @PostMapping
    public ResponseEntity<StaffDTO> create(@Valid @RequestBody StaffDTO dto) {
        log.info("POST /api/staff - Creating staff: {} {}", dto.getName(), dto.getSurname());

        try {
            StaffDTO created = staffService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating staff: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing staff
     */
    @PutMapping("/{id}")
    public ResponseEntity<StaffDTO> update(@PathVariable Long id, @Valid @RequestBody StaffDTO dto) {
        log.info("PUT /api/staff/{} - Updating staff", id);

        try {
            StaffDTO updated = staffService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating staff: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete staff
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/staff/{} - Deleting staff", id);

        try {
            staffService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting staff: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get staff by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StaffDTO> findById(@PathVariable Long id) {
        log.info("GET /api/staff/{} - Finding staff by ID", id);

        try {
            StaffDTO staff = staffService.findById(id);
            return ResponseEntity.ok(staff);
        } catch (IllegalArgumentException e) {
            log.error("Staff not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active staff
     */
    @GetMapping
    public ResponseEntity<List<StaffDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/staff - Finding all staff (includeInactive: {})", includeInactive);

        List<StaffDTO> staffList = includeInactive
                ? staffService.findAllIncludingInactive()
                : staffService.findAll();

        return ResponseEntity.ok(staffList);
    }

    /**
     * Search staff by name or surname
     */
    @GetMapping("/search")
    public ResponseEntity<List<StaffDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/staff/search - Searching staff by name: {}", name);

        List<StaffDTO> staffList = staffService.searchByName(name, includeInactive);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff by department
     */
    @GetMapping("/department/{departmentId}")
    public ResponseEntity<List<StaffDTO>> findByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/staff/department/{} - Finding staff by department", departmentId);

        List<StaffDTO> staffList = staffService.findByDepartment(departmentId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff by squad
     */
    @GetMapping("/squad/{squadId}")
    public ResponseEntity<List<StaffDTO>> findBySquad(@PathVariable Long squadId) {
        log.info("GET /api/staff/squad/{} - Finding staff by squad", squadId);

        List<StaffDTO> staffList = staffService.findBySquad(squadId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff by qualification
     */
    @GetMapping("/qualification/{qualificationId}")
    public ResponseEntity<List<StaffDTO>> findByQualification(@PathVariable Long qualificationId) {
        log.info("GET /api/staff/qualification/{} - Finding staff by qualification", qualificationId);

        List<StaffDTO> staffList = staffService.findByQualification(qualificationId);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff with all specified qualifications
     */
    @PostMapping("/qualifications/all")
    public ResponseEntity<List<StaffDTO>> findByAllQualifications(@RequestBody List<Long> qualificationIds) {
        log.info("POST /api/staff/qualifications/all - Finding staff with all qualifications: {}", qualificationIds);

        List<StaffDTO> staffList = staffService.findByAllQualifications(qualificationIds);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff with any of the specified qualifications
     */
    @PostMapping("/qualifications/any")
    public ResponseEntity<List<StaffDTO>> findByAnyQualification(@RequestBody List<Long> qualificationIds) {
        log.info("POST /api/staff/qualifications/any - Finding staff with any qualifications: {}", qualificationIds);

        List<StaffDTO> staffList = staffService.findByAnyQualification(qualificationIds);
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff without qualifications
     */
    @GetMapping("/without-qualifications")
    public ResponseEntity<List<StaffDTO>> findStaffWithoutQualifications() {
        log.info("GET /api/staff/without-qualifications - Finding staff without qualifications");

        List<StaffDTO> staffList = staffService.findStaffWithoutQualifications();
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff with day off rules
     */
    @GetMapping("/with-day-off-rules")
    public ResponseEntity<List<StaffDTO>> findStaffWithDayOffRules() {
        log.info("GET /api/staff/with-day-off-rules - Finding staff with day off rules");

        List<StaffDTO> staffList = staffService.findStaffWithDayOffRules();
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff with constraint overrides
     */
    @GetMapping("/with-constraint-overrides")
    public ResponseEntity<List<StaffDTO>> findStaffWithConstraintOverrides() {
        log.info("GET /api/staff/with-constraint-overrides - Finding staff with constraint overrides");

        List<StaffDTO> staffList = staffService.findStaffWithConstraintOverrides();
        return ResponseEntity.ok(staffList);
    }

    /**
     * Find staff by registration code
     */
    @GetMapping("/registration/{registrationCode}")
    public ResponseEntity<StaffDTO> findByRegistrationCode(@PathVariable String registrationCode) {
        log.info("GET /api/staff/registration/{} - Finding staff by registration code", registrationCode);

        try {
            StaffDTO staff = staffService.findByRegistrationCode(registrationCode);
            return ResponseEntity.ok(staff);
        } catch (IllegalArgumentException e) {
            log.error("Staff not found by registration code: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Find staff by email
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<StaffDTO> findByEmail(@PathVariable String email) {
        log.info("GET /api/staff/email/{} - Finding staff by email", email);

        try {
            StaffDTO staff = staffService.findByEmail(email);
            return ResponseEntity.ok(staff);
        } catch (IllegalArgumentException e) {
            log.error("Staff not found by email: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get active staff count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/staff/count - Getting active staff count");

        long count = staffService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get staff count by department
     */
    @GetMapping("/count/department/{departmentId}")
    public ResponseEntity<Long> getCountByDepartment(@PathVariable Long departmentId) {
        log.info("GET /api/staff/count/department/{} - Getting staff count by department", departmentId);

        long count = staffService.getCountByDepartment(departmentId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get staff count by squad
     */
    @GetMapping("/count/squad/{squadId}")
    public ResponseEntity<Long> getCountBySquad(@PathVariable Long squadId) {
        log.info("GET /api/staff/count/squad/{} - Getting staff count by squad", squadId);

        long count = staffService.getCountBySquad(squadId);
        return ResponseEntity.ok(count);
    }

    /**
     * Get staff by IDs (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<StaffDTO>> findByIds(@RequestBody List<Long> ids) {
        log.info("POST /api/staff/bulk - Finding staff by IDs: {}", ids);

        List<StaffDTO> staffList = staffService.findByIds(ids);
        return ResponseEntity.ok(staffList);
    }
}
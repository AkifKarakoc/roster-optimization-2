package com.rosteroptimization.controller;

import com.rosteroptimization.dto.DepartmentDTO;
import com.rosteroptimization.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Create new department
     */
    @PostMapping
    public ResponseEntity<DepartmentDTO> create(@Valid @RequestBody DepartmentDTO dto) {
        log.info("POST /api/departments - Creating department: {}", dto.getName());

        try {
            DepartmentDTO created = departmentService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating department: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing department
     */
    @PutMapping("/{id}")
    public ResponseEntity<DepartmentDTO> update(@PathVariable Long id, @Valid @RequestBody DepartmentDTO dto) {
        log.info("PUT /api/departments/{} - Updating department", id);

        try {
            DepartmentDTO updated = departmentService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating department: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete department
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/departments/{} - Deleting department", id);

        try {
            departmentService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting department: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get department by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<DepartmentDTO> findById(@PathVariable Long id) {
        log.info("GET /api/departments/{} - Finding department by ID", id);

        try {
            DepartmentDTO department = departmentService.findById(id);
            return ResponseEntity.ok(department);
        } catch (IllegalArgumentException e) {
            log.error("Department not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active departments
     */
    @GetMapping
    public ResponseEntity<List<DepartmentDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/departments - Finding all departments (includeInactive: {})", includeInactive);

        List<DepartmentDTO> departments = includeInactive
                ? departmentService.findAllIncludingInactive()
                : departmentService.findAll();

        return ResponseEntity.ok(departments);
    }

    /**
     * Search departments by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<DepartmentDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/departments/search - Searching departments by name: {}", name);

        List<DepartmentDTO> departments = departmentService.searchByName(name, includeInactive);
        return ResponseEntity.ok(departments);
    }

    /**
     * Get active department count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/departments/count - Getting active department count");

        long count = departmentService.getActiveCount();
        return ResponseEntity.ok(count);
    }
}
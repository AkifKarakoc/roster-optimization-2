package com.rosteroptimization.controller;

import com.rosteroptimization.dto.QualificationDTO;
import com.rosteroptimization.service.QualificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/qualifications")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class QualificationController {

    private final QualificationService qualificationService;

    /**
     * Create new qualification
     */
    @PostMapping
    public ResponseEntity<QualificationDTO> create(@Valid @RequestBody QualificationDTO dto) {
        log.info("POST /api/qualifications - Creating qualification: {}", dto.getName());

        try {
            QualificationDTO created = qualificationService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating qualification: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing qualification
     */
    @PutMapping("/{id}")
    public ResponseEntity<QualificationDTO> update(@PathVariable Long id, @Valid @RequestBody QualificationDTO dto) {
        log.info("PUT /api/qualifications/{} - Updating qualification", id);

        try {
            QualificationDTO updated = qualificationService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating qualification: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete qualification
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/qualifications/{} - Deleting qualification", id);

        try {
            qualificationService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting qualification: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get qualification by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<QualificationDTO> findById(@PathVariable Long id) {
        log.info("GET /api/qualifications/{} - Finding qualification by ID", id);

        try {
            QualificationDTO qualification = qualificationService.findById(id);
            return ResponseEntity.ok(qualification);
        } catch (IllegalArgumentException e) {
            log.error("Qualification not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active qualifications
     */
    @GetMapping
    public ResponseEntity<List<QualificationDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/qualifications - Finding all qualifications (includeInactive: {})", includeInactive);

        List<QualificationDTO> qualifications = includeInactive
                ? qualificationService.findAllIncludingInactive()
                : qualificationService.findAll();

        return ResponseEntity.ok(qualifications);
    }

    /**
     * Search qualifications by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<QualificationDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/qualifications/search - Searching qualifications by name: {}", name);

        List<QualificationDTO> qualifications = qualificationService.searchByName(name, includeInactive);
        return ResponseEntity.ok(qualifications);
    }

    /**
     * Get active qualification count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/qualifications/count - Getting active qualification count");

        long count = qualificationService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Get qualifications by IDs (for bulk operations)
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<QualificationDTO>> findByIds(@RequestBody List<Long> ids) {
        log.info("POST /api/qualifications/bulk - Finding qualifications by IDs: {}", ids);

        List<QualificationDTO> qualifications = qualificationService.findByIds(ids);
        return ResponseEntity.ok(qualifications);
    }
}
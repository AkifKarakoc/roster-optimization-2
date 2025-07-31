package com.rosteroptimization.controller;

import com.rosteroptimization.dto.SquadWorkingPatternDTO;
import com.rosteroptimization.service.SquadWorkingPatternService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/squad-working-patterns")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class SquadWorkingPatternController {

    private final SquadWorkingPatternService squadWorkingPatternService;

    /**
     * Create new squad working pattern
     */
    @PostMapping
    public ResponseEntity<SquadWorkingPatternDTO> create(@Valid @RequestBody SquadWorkingPatternDTO dto) {
        log.info("POST /api/squad-working-patterns - Creating squad working pattern: {}", dto.getName());

        try {
            SquadWorkingPatternDTO created = squadWorkingPatternService.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            log.error("Error creating squad working pattern: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Update existing squad working pattern
     */
    @PutMapping("/{id}")
    public ResponseEntity<SquadWorkingPatternDTO> update(@PathVariable Long id, @Valid @RequestBody SquadWorkingPatternDTO dto) {
        log.info("PUT /api/squad-working-patterns/{} - Updating squad working pattern", id);

        try {
            SquadWorkingPatternDTO updated = squadWorkingPatternService.update(id, dto);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Error updating squad working pattern: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Soft delete squad working pattern
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /api/squad-working-patterns/{} - Deleting squad working pattern", id);

        try {
            squadWorkingPatternService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.error("Error deleting squad working pattern: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get squad working pattern by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SquadWorkingPatternDTO> findById(@PathVariable Long id) {
        log.info("GET /api/squad-working-patterns/{} - Finding squad working pattern by ID", id);

        try {
            SquadWorkingPatternDTO squadWorkingPattern = squadWorkingPatternService.findById(id);
            return ResponseEntity.ok(squadWorkingPattern);
        } catch (IllegalArgumentException e) {
            log.error("Squad working pattern not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all active squad working patterns
     */
    @GetMapping
    public ResponseEntity<List<SquadWorkingPatternDTO>> findAll(
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/squad-working-patterns - Finding all squad working patterns (includeInactive: {})", includeInactive);

        List<SquadWorkingPatternDTO> squadWorkingPatterns = includeInactive
                ? squadWorkingPatternService.findAllIncludingInactive()
                : squadWorkingPatternService.findAll();

        return ResponseEntity.ok(squadWorkingPatterns);
    }

    /**
     * Search squad working patterns by name
     */
    @GetMapping("/search")
    public ResponseEntity<List<SquadWorkingPatternDTO>> search(
            @RequestParam String name,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        log.info("GET /api/squad-working-patterns/search - Searching squad working patterns by name: {}", name);

        List<SquadWorkingPatternDTO> squadWorkingPatterns = squadWorkingPatternService.searchByName(name, includeInactive);
        return ResponseEntity.ok(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns by cycle length
     */
    @GetMapping("/cycle-length/{cycleLength}")
    public ResponseEntity<List<SquadWorkingPatternDTO>> findByCycleLength(@PathVariable Integer cycleLength) {
        log.info("GET /api/squad-working-patterns/cycle-length/{} - Finding squad working patterns by cycle length", cycleLength);

        List<SquadWorkingPatternDTO> squadWorkingPatterns = squadWorkingPatternService.findByCycleLength(cycleLength);
        return ResponseEntity.ok(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns containing specific shift
     */
    @GetMapping("/shift/{shiftName}")
    public ResponseEntity<List<SquadWorkingPatternDTO>> findByShiftName(@PathVariable String shiftName) {
        log.info("GET /api/squad-working-patterns/shift/{} - Finding squad working patterns containing shift", shiftName);

        List<SquadWorkingPatternDTO> squadWorkingPatterns = squadWorkingPatternService.findByShiftName(shiftName);
        return ResponseEntity.ok(squadWorkingPatterns);
    }

    /**
     * Find squad working patterns by cycle length range
     */
    @GetMapping("/cycle-length-range")
    public ResponseEntity<List<SquadWorkingPatternDTO>> findByCycleLengthRange(
            @RequestParam Integer minLength,
            @RequestParam Integer maxLength) {
        log.info("GET /api/squad-working-patterns/cycle-length-range - Finding squad working patterns by cycle length range: {} - {}", minLength, maxLength);

        try {
            List<SquadWorkingPatternDTO> squadWorkingPatterns = squadWorkingPatternService.findByCycleLengthRange(minLength, maxLength);
            return ResponseEntity.ok(squadWorkingPatterns);
        } catch (IllegalArgumentException e) {
            log.error("Error finding squad working patterns by cycle length range: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get active squad working pattern count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getActiveCount() {
        log.info("GET /api/squad-working-patterns/count - Getting active squad working pattern count");

        long count = squadWorkingPatternService.getActiveCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Validate pattern format and content
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validatePattern(@RequestBody ValidatePatternRequest request) {
        log.info("POST /api/squad-working-patterns/validate - Validating pattern: {}", request.getShiftPattern());

        try {
            List<String> errors = squadWorkingPatternService.validatePattern(request.getShiftPattern(), request.getCycleLength());

            if (errors.isEmpty()) {
                return ResponseEntity.ok(new ValidationResponse(true, "Pattern is valid", null));
            } else {
                return ResponseEntity.ok(new ValidationResponse(false, "Pattern has validation errors", errors));
            }
        } catch (Exception e) {
            log.error("Error validating pattern: {}", e.getMessage());
            return ResponseEntity.ok(new ValidationResponse(false, "Validation failed: " + e.getMessage(), null));
        }
    }

    // Inner classes for validation request/response
    public static class ValidatePatternRequest {
        private String shiftPattern;
        private Integer cycleLength;

        // Getters and setters
        public String getShiftPattern() { return shiftPattern; }
        public void setShiftPattern(String shiftPattern) { this.shiftPattern = shiftPattern; }
        public Integer getCycleLength() { return cycleLength; }
        public void setCycleLength(Integer cycleLength) { this.cycleLength = cycleLength; }
    }

    public static class ValidationResponse {
        private boolean valid;
        private String message;
        private List<String> errors;

        public ValidationResponse(boolean valid, String message, List<String> errors) {
            this.valid = valid;
            this.message = message;
            this.errors = errors;
        }

        // Getters and setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
    }
}
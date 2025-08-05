package com.rosteroptimization.controller;

import com.rosteroptimization.dto.RosterPlanDTO;
import com.rosteroptimization.mapper.RosterPlanMapper;
import com.rosteroptimization.service.RosterService;
import com.rosteroptimization.service.optimization.algorithm.OptimizationException;
import com.rosteroptimization.service.optimization.model.RosterPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * REST Controller for roster optimization operations
 */
@RestController
@RequestMapping("/api/roster")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class RosterController {

    private final RosterService rosterService;
    private final RosterPlanMapper rosterPlanMapper;

    /**
     * Generate a new roster plan using optimization algorithms
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateRosterPlan(@Valid @RequestBody RosterService.RosterGenerationRequest request) {
        try {
            log.info("Received roster generation request for department: {}", request.getDepartmentId());

            RosterPlan rosterPlan = rosterService.generateRosterPlan(request);
            RosterPlanDTO rosterPlanDTO = rosterPlanMapper.toDTO(rosterPlan);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Roster plan generated successfully",
                    rosterPlanDTO
            ));

        } catch (OptimizationException e) {
            log.error("Optimization failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ApiResponse<>(false, "Optimization failed: " + e.getMessage(), null));

        } catch (Exception e) {
            log.error("Unexpected error during roster generation: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Internal server error: " + e.getMessage(), null));
        }
    }

    /**
     * Get list of available optimization algorithms
     */
    @GetMapping("/algorithms")
    public ResponseEntity<ApiResponse<List<RosterService.AlgorithmInfo>>> getAvailableAlgorithms() {
        try {
            List<RosterService.AlgorithmInfo> algorithms = rosterService.getAvailableAlgorithms();

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Available algorithms retrieved successfully",
                    algorithms
            ));

        } catch (Exception e) {
            log.error("Error retrieving available algorithms: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving algorithms: " + e.getMessage(), null));
        }
    }

    /**
     * Get estimated execution time for optimization request
     */
    @PostMapping("/estimate-time")
    public ResponseEntity<ApiResponse<Long>> getEstimatedExecutionTime(@Valid @RequestBody RosterService.RosterGenerationRequest request) {
        try {
            long estimatedTime = rosterService.getEstimatedExecutionTime(request);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Estimated execution time calculated",
                    estimatedTime
            ));

        } catch (Exception e) {
            log.error("Error estimating execution time: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error estimating time: " + e.getMessage(), null));
        }
    }

    /**
     * Validate an existing roster plan
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<RosterService.RosterValidationResult>> validateRosterPlan(@Valid @RequestBody RosterPlan rosterPlan) {
        try {
            RosterService.RosterValidationResult validationResult = rosterService.validateRosterPlan(rosterPlan);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Roster plan validated",
                    validationResult
            ));

        } catch (Exception e) {
            log.error("Error validating roster plan: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error validating roster: " + e.getMessage(), null));
        }
    }

    /**
     * Get roster statistics for analysis
     */
    @PostMapping("/statistics")
    public ResponseEntity<ApiResponse<RosterService.RosterStatistics>> getRosterStatistics(@Valid @RequestBody RosterPlan rosterPlan) {
        try {
            RosterService.RosterStatistics statistics = rosterService.getRosterStatistics(rosterPlan);

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Roster statistics calculated",
                    statistics
            ));

        } catch (Exception e) {
            log.error("Error calculating roster statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error calculating statistics: " + e.getMessage(), null));
        }
    }

    /**
     * Get default parameters for specific algorithm
     */
    @GetMapping("/algorithms/{algorithmType}/parameters")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlgorithmParameters(@PathVariable String algorithmType) {
        try {
            List<RosterService.AlgorithmInfo> algorithms = rosterService.getAvailableAlgorithms();

            RosterService.AlgorithmInfo algorithm = algorithms.stream()
                    .filter(alg -> alg.getName().equals(algorithmType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Algorithm not found: " + algorithmType));

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Algorithm parameters retrieved",
                    algorithm.getDefaultParameters()
            ));

        } catch (Exception e) {
            log.error("Error retrieving algorithm parameters: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Error retrieving parameters: " + e.getMessage(), null));
        }
    }

    /**
     * Health check endpoint for optimization service
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        try {
            List<RosterService.AlgorithmInfo> algorithms = rosterService.getAvailableAlgorithms();

            String message = String.format("Roster optimization service is healthy. %d algorithms available: %s",
                    algorithms.size(),
                    algorithms.stream().map(RosterService.AlgorithmInfo::getName).toList());

            return ResponseEntity.ok(new ApiResponse<>(true, message, "OK"));

        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ApiResponse<>(false, "Service unhealthy: " + e.getMessage(), "ERROR"));
        }
    }

    /**
     * Test genetic algorithm with sample data
     */
    @PostMapping("/test-genetic-algorithm")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testGeneticAlgorithm() {
        try {
            log.info("Testing genetic algorithm with sample data...");

            // Create a test request for Acil Servis department
            RosterService.RosterGenerationRequest request = new RosterService.RosterGenerationRequest();
            request.setDepartmentId(1L); // Acil Servis
            request.setStartDate(LocalDate.of(2025, 8, 1));
            request.setEndDate(LocalDate.of(2025, 8, 7)); // 1 week
            request.setAlgorithmType("GENETIC_ALGORITHM");
            request.setMaxExecutionTimeMinutes(2);
            request.setEnableParallelProcessing(true);

            // Set genetic algorithm parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("populationSize", 50);
            parameters.put("maxGenerations", 100);
            parameters.put("mutationRate", 0.01);
            parameters.put("crossoverRate", 0.8);
            parameters.put("eliteSize", 5);
            request.setAlgorithmParameters(parameters);

            // Generate roster plan
            RosterPlan rosterPlan = rosterService.generateRosterPlan(request);

            // Prepare response
            Map<String, Object> result = new HashMap<>();
            result.put("rosterPlan", rosterPlan);
            result.put("request", request);
            result.put("executionTime", System.currentTimeMillis());
            result.put("totalAssignments", rosterPlan.getAssignments().size());
            result.put("fitnessScore", rosterPlan.getFitnessScore());
            result.put("isFeasible", rosterPlan.isFeasible());

            return ResponseEntity.ok(new ApiResponse<>(
                    true,
                    "Genetic algorithm test completed successfully",
                    result
            ));

        } catch (Exception e) {
            log.error("Genetic algorithm test failed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(false, "Test failed: " + e.getMessage(), null));
        }
    }

    /**
     * Generic API Response wrapper class
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(String message, T data) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }
}
package com.rosteroptimization.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @PostMapping("/roster/generate")
    public ResponseEntity<Map<String, Object>> testRosterGenerate(@RequestBody TestRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Genetic Algorithm test completed successfully!");
        response.put("algorithm", request.getAlgorithmType());
        response.put("departmentId", request.getDepartmentId());
        response.put("startDate", request.getStartDate());
        response.put("endDate", request.getEndDate());
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Test controller is working");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }

    // Test request class
    public static class TestRequest {
        private String algorithmType;
        private Long departmentId;
        private LocalDate startDate;
        private LocalDate endDate;

        // Getters and setters
        public String getAlgorithmType() {
            return algorithmType;
        }

        public void setAlgorithmType(String algorithmType) {
            this.algorithmType = algorithmType;
        }

        public Long getDepartmentId() {
            return departmentId;
        }

        public void setDepartmentId(Long departmentId) {
            this.departmentId = departmentId;
        }

        public LocalDate getStartDate() {
            return startDate;
        }

        public void setStartDate(LocalDate startDate) {
            this.startDate = startDate;
        }

        public LocalDate getEndDate() {
            return endDate;
        }

        public void setEndDate(LocalDate endDate) {
            this.endDate = endDate;
        }
    }
}

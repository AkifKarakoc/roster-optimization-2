package com.rosteroptimization.dto.excel;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ImportResult {
    private String sessionId;
    private boolean success;
    private String message;
    private LocalDateTime executedAt;
    private Map<String, Integer> appliedCounts;
    private int totalProcessed;
    private int successfulOperations;
    private int failedOperations;

    public ImportResult() {
        this.executedAt = LocalDateTime.now();
    }
}
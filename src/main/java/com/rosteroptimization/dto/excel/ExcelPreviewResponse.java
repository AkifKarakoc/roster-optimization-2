package com.rosteroptimization.dto.excel;

import com.rosteroptimization.enums.ImportMode;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExcelPreviewResponse {
    private String sessionId;
    private ImportMode mode;
    private String entityType;
    private int totalRows;
    private int validRows;
    private int invalidRows;
    private List<ExcelRowResult<?>> results;
    private Map<String, Integer> operationCounts;
    private boolean canProceed;

    public ExcelPreviewResponse() {
        this.canProceed = false;
    }
}
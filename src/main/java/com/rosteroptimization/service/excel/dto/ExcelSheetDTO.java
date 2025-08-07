package com.rosteroptimization.service.excel.dto;

import com.rosteroptimization.service.excel.ExcelEntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO representing a single Excel sheet with parsed data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcelSheetDTO {

    private String sheetName;
    private ExcelEntityType entityType;
    private List<String> headers;
    private List<ExcelRowDTO> rows;
    private int totalRows;

    // Statistics (populated during validation)
    private int validRows;
    private int errorRows;
    private int warningRows;

    // Processing info
    private boolean hasOperationColumn;
    private String mainIdColumn; // First column name (main entity ID)

    /**
     * Get count of rows that can be imported (no blocking errors)
     */
    public int getImportableRows() {
        if (rows == null) return 0;
        return (int) rows.stream().filter(ExcelRowDTO::isCanImport).count();
    }

    /**
     * Check if sheet has any data to process
     */
    public boolean hasData() {
        return rows != null && !rows.isEmpty();
    }

    /**
     * Get operation column name if exists
     */
    public String getOperationColumnName() {
        if (headers == null) return null;

        return headers.stream()
                .filter(header -> header.equalsIgnoreCase("OPERATION") ||
                        header.equalsIgnoreCase("OP") ||
                        header.equalsIgnoreCase("ACTION"))
                .findFirst()
                .orElse(null);
    }

    /**
     * Update statistics after validation
     */
    public void updateStatistics() {
        if (rows == null) {
            this.validRows = 0;
            this.errorRows = 0;
            this.warningRows = 0;
            return;
        }

        this.validRows = 0;
        this.errorRows = 0;
        this.warningRows = 0;

        for (ExcelRowDTO row : rows) {
            if (row.hasErrors()) {
                if (row.hasBlockingErrors()) {
                    this.errorRows++;
                } else {
                    this.warningRows++;
                }
            } else {
                this.validRows++;
            }
        }

        // Set operation column flag
        this.hasOperationColumn = getOperationColumnName() != null;

        // Set main ID column (first column)
        this.mainIdColumn = headers != null && !headers.isEmpty() ? headers.get(0) : null;
    }
}
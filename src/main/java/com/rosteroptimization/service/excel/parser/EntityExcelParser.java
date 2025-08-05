package com.rosteroptimization.service.excel.parser;

import com.rosteroptimization.dto.excel.ExcelRowResult;
import org.apache.poi.ss.usermodel.Sheet;
import java.util.List;

public interface EntityExcelParser<T> {

    /**
     * Parse all rows from Excel sheet
     */
    List<ExcelRowResult<T>> parseRows(Sheet sheet);

    /**
     * Get expected headers for this entity
     */
    String[] getExpectedHeaders();

    /**
     * Get entity type name
     */
    String getEntityType();

    /**
     * Validate single row data
     */
    void validateRow(ExcelRowResult<T> rowResult);
}
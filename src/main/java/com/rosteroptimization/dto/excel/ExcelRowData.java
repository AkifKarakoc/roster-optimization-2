package com.rosteroptimization.dto.excel;

import lombok.Data;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import java.util.HashMap;
import java.util.Map;

@Data
public class ExcelRowData {
    private int rowNumber;
    private Map<String, String> cellValues;

    public ExcelRowData(Row row, String[] headers) {
        this.rowNumber = row.getRowNum();
        this.cellValues = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.getCell(i);
            String value = getCellValueAsString(cell);
            cellValues.put(headers[i], value);
        }
    }

    public String getValue(String header) {
        return cellValues.getOrDefault(header, "");
    }

    public boolean hasValue(String header) {
        String value = getValue(header);
        return value != null && !value.trim().isEmpty();
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}
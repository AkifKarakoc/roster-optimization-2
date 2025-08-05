package com.rosteroptimization.dto.excel;

import com.rosteroptimization.enums.ExcelOperation;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class ExcelRowResult<T> {
    private int rowNumber;
    private ExcelOperation operation;
    private T data;
    private boolean valid;
    private List<ValidationError> errors;

    public ExcelRowResult() {
        this.errors = new ArrayList<>();
        this.valid = true;
    }

    public void addError(String field, String message) {
        this.errors.add(new ValidationError(field, message));
        this.valid = false;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
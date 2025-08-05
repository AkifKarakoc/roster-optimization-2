package com.rosteroptimization.enums;

public enum EntityImportOrder {
    DEPARTMENT(1),
    QUALIFICATION(1),
    WORKING_PERIOD(1),
    CONSTRAINT(2),
    SQUAD_WORKING_PATTERN(2),
    SQUAD(3),
    SHIFT(3),
    STAFF(4),
    TASK(4),
    DAY_OFF_RULE(5),
    CONSTRAINT_OVERRIDE(5);

    private final int order;

    EntityImportOrder(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
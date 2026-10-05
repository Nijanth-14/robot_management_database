package com.robotmonitor.model;

import java.util.Arrays;

public enum TaskStatus {
    PENDING("Pending"), IN_PROGRESS("In Progress"), COMPLETED("Completed"), CANCELLED("Cancelled");

    private final String databaseValue;
    TaskStatus(String value) { databaseValue = value; }
    public String databaseValue() { return databaseValue; }
    public static TaskStatus fromDatabase(String value) {
        return Arrays.stream(values()).filter(s -> s.databaseValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown task status: " + value));
    }
    @Override public String toString() { return databaseValue; }
}

package com.robotmonitor.model;

import java.util.Arrays;

public enum Priority {
    LOW("Low"), MEDIUM("Medium"), HIGH("High");

    private final String databaseValue;
    Priority(String value) { databaseValue = value; }
    public String databaseValue() { return databaseValue; }
    public static Priority fromDatabase(String value) {
        return Arrays.stream(values()).filter(s -> s.databaseValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown priority: " + value));
    }
    @Override public String toString() { return databaseValue; }
}

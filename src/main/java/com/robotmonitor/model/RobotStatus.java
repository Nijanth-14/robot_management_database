package com.robotmonitor.model;

import java.util.Arrays;

public enum RobotStatus {
    AVAILABLE("Available"), BUSY("Busy"), CHARGING("Charging"),
    OFFLINE("Offline"), MAINTENANCE("Maintenance");

    private final String databaseValue;
    RobotStatus(String value) { databaseValue = value; }
    public String databaseValue() { return databaseValue; }
    public static RobotStatus fromDatabase(String value) {
        return Arrays.stream(values()).filter(s -> s.databaseValue.equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown robot status: " + value));
    }
    @Override public String toString() { return databaseValue; }
}

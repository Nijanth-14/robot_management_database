package com.robotmonitor.model;

import java.util.Objects;

/** Immutable database record; created_at and updated_at are managed by MySQL. */
public record Robot(String robotId, String robotName, String robotType, String location,
                    RobotStatus status, int batteryLevel) {
    public Robot {
        robotId = Validation.text(robotId, "Robot ID", 20);
        robotName = Validation.text(robotName, "Robot name", 100);
        robotType = Validation.text(robotType, "Robot type", 50);
        location = Validation.text(location, "Location", 100);
        Objects.requireNonNull(status, "Robot status is required");
        if (batteryLevel < 0 || batteryLevel > 100) {
            throw new IllegalArgumentException("Battery level must be between 0 and 100");
        }
    }
}

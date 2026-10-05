package com.robotmonitor.model;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** robotId may be null for an unassigned task. */
public record Task(String taskId, String taskName, String description, Priority priority,
                   TaskStatus taskStatus, String robotId) {
    public Task {
        taskId = Validation.text(taskId, "Task ID", 20);
        taskName = Validation.text(taskName, "Task name", 150);
        Objects.requireNonNull(priority, "Task priority is required");
        Objects.requireNonNull(taskStatus, "Task status is required");
        if (description != null && description.getBytes(StandardCharsets.UTF_8).length > 65535) {
            throw new IllegalArgumentException("Description exceeds MySQL TEXT capacity");
        }
        if (robotId != null) robotId = Validation.text(robotId, "Robot ID", 20);
        if (taskStatus == TaskStatus.IN_PROGRESS && robotId == null) {
            throw new IllegalArgumentException("An in-progress task needs an assigned robot");
        }
    }
}

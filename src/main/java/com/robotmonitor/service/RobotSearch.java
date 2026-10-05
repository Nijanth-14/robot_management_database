package com.robotmonitor.service;

import com.robotmonitor.model.Robot;
import com.robotmonitor.model.RobotStatus;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RobotSearch {
    private RobotSearch() {}

    /** Null status matches all statuses; null/blank text matches all robots. */
    public static List<Robot> filter(List<Robot> robots, String text, RobotStatus status) {
        String query = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
        return robots.stream()
                .filter(r -> status == null || r.status() == status)
                .filter(r -> r.robotId().toLowerCase(Locale.ROOT).contains(query)
                        || r.robotName().toLowerCase(Locale.ROOT).contains(query)
                        || r.robotType().toLowerCase(Locale.ROOT).contains(query)
                        || r.location().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparing(Robot::robotName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Robot::robotId))
                .toList();
    }
}

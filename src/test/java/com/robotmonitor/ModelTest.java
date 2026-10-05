package com.robotmonitor;

import com.robotmonitor.model.*;
import com.robotmonitor.service.RobotSearch;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ModelTest {
    @Test void rejectsInvalidRobotInput() {
        assertThrows(IllegalArgumentException.class, () -> robot("R1", "", 50));
        assertThrows(IllegalArgumentException.class, () -> robot("R1", "Test", -1));
        assertThrows(IllegalArgumentException.class, () -> robot("R1", "Test", 101));
        assertThrows(IllegalArgumentException.class, () -> robot("x".repeat(21), "Test", 50));
        assertEquals(0, robot("R1", "Test", 0).batteryLevel());
        assertEquals(100, robot("R1", "Test", 100).batteryLevel());
    }

    @Test void permitsPendingUnassignedButRejectsActiveUnassignedTask() {
        assertNull(new Task("T1", "Clean", null, Priority.LOW, TaskStatus.PENDING, null).robotId());
        assertThrows(IllegalArgumentException.class,
                () -> new Task("T1", "Clean", null, Priority.LOW, TaskStatus.IN_PROGRESS, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Task("T1", "Clean", "é".repeat(32768), Priority.LOW, TaskStatus.PENDING, null));
    }

    @Test void filtersAndSortsWithoutChangingInput() {
        Robot zulu = robot("R2", "Zulu", 80);
        Robot alpha = robot("R1", "alpha", 50);
        Robot busy = new Robot("R3", "Other", "Transport", "Zone B", RobotStatus.BUSY, 50);
        List<Robot> input = List.of(zulu, alpha, busy);
        assertEquals(List.of(alpha, zulu), RobotSearch.filter(input, " ZONE A ", RobotStatus.AVAILABLE));
        assertEquals(List.of(zulu), RobotSearch.filter(input, "r2", null));
        assertEquals(List.of(zulu, alpha, busy), input);
    }

    private Robot robot(String id, String name, int battery) {
        return new Robot(id, name, "Transport", "Zone A", RobotStatus.AVAILABLE, battery);
    }
}

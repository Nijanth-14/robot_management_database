package com.robotmonitor;

import com.robotmonitor.db.*;
import com.robotmonitor.model.*;
import com.robotmonitor.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in only against the isolated robot_monitoring_test schema. */
@EnabledIfEnvironmentVariable(named = "DB_TEST_URL", matches = ".+")
class RepositoryIntegrationTest {
    private Database database;
    private RobotRepository robots;
    private TaskRepository tasks;
    private String robotId;
    private String taskId;

    @BeforeEach void setup() throws Exception {
        database = new Database(new DatabaseConfig(System.getenv("DB_TEST_URL"),
                System.getenv().getOrDefault("DB_TEST_USER", "root"),
                System.getenv().getOrDefault("DB_TEST_PASSWORD", "")));
        try (Connection c = database.open()) {
            if (!"robot_monitoring_test".equals(c.getCatalog())) {
                throw new IllegalStateException("Integration tests require robot_monitoring_test");
            }
        }
        robots = new RobotRepository(database);
        tasks = new TaskRepository(database);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        robotId = "R" + suffix;
        taskId = "T" + suffix;
    }

    @AfterEach void cleanup() throws Exception {
        // Only remove records created by this test, never seeded or user data.
        if (taskId != null) tasks.delete(taskId);
        if (robotId != null) robots.delete(robotId);
    }

    @Test void robotCrudRoundTripAndNoOpUpdate() throws Exception {
        Robot original = robot("O'Brien'); DROP TABLE robot; --", 90);
        robots.create(original);
        assertEquals(original, robots.findById(robotId).orElseThrow());
        assertTrue(robots.findAll().contains(original));
        assertTrue(robots.update(original));
        Robot changed = robot("Updated name", 0);
        assertTrue(robots.update(changed));
        assertEquals(changed, robots.findById(robotId).orElseThrow());
        assertTrue(robots.delete(robotId));
        assertTrue(robots.findById(robotId).isEmpty());
        assertFalse(robots.delete(robotId));
        assertFalse(robots.update(changed));
    }

    @Test void taskCrudAssignmentAndUnassignment() throws Exception {
        robots.create(robot("Test robot", 100));
        Task pending = task(TaskStatus.PENDING, null);
        tasks.create(pending);
        assertEquals(pending, tasks.findById(taskId).orElseThrow());
        assertTrue(tasks.findAll().contains(pending));
        Task active = task(TaskStatus.IN_PROGRESS, robotId);
        assertTrue(tasks.update(active));
        assertTrue(tasks.findByRobotId(robotId).contains(active));
        assertEquals(active, tasks.findById(taskId).orElseThrow());
        assertTrue(tasks.update(pending));
        assertNull(tasks.findById(taskId).orElseThrow().robotId());
        assertTrue(tasks.delete(taskId));
        assertTrue(tasks.findById(taskId).isEmpty());
        assertFalse(tasks.delete(taskId));
        assertFalse(tasks.update(pending));
    }

    @Test void rejectsUnknownRobotAssignment() {
        assertThrows(SQLException.class, () -> tasks.create(task(TaskStatus.PENDING, robotId)));
    }

    @Test void preventsDeletingReferencedRobot() throws Exception {
        robots.create(robot("Test robot", 40));
        tasks.create(task(TaskStatus.COMPLETED, robotId));
        assertThrows(SQLException.class, () -> robots.delete(robotId));
        assertTrue(robots.findById(robotId).isPresent());
    }

    @Test void rejectsDuplicateIdWithoutOverwriting() throws Exception {
        Robot original = robot("Original", 50);
        robots.create(original);
        assertThrows(SQLException.class, () -> robots.create(robot("Duplicate", 10)));
        assertEquals(original, robots.findById(robotId).orElseThrow());
    }

    @Test void databaseEnforcesBatteryConstraint() throws Exception {
        robots.create(robot("Test robot", 50));
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE robot SET battery_level=101 WHERE robot_id=?")) {
            s.setString(1, robotId);
            assertThrows(SQLException.class, s::executeUpdate);
        }
        assertEquals(50, robots.findById(robotId).orElseThrow().batteryLevel());
    }

    @Test void databaseRejectsUnassignedActiveTask() throws Exception {
        tasks.create(task(TaskStatus.PENDING, null));
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE task SET task_status='In Progress' WHERE task_id=?")) {
            s.setString(1, taskId);
            assertThrows(SQLException.class, s::executeUpdate);
        }
        assertEquals(TaskStatus.PENDING, tasks.findById(taskId).orElseThrow().taskStatus());
    }

    private Robot robot(String name, int battery) {
        return new Robot(robotId, name, "Inspection", "Test zone", RobotStatus.AVAILABLE, battery);
    }
    private Task task(TaskStatus status, String assignedId) {
        return new Task(taskId, "Test task", "Test description", Priority.HIGH, status, assignedId);
    }
}

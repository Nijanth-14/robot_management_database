package com.robotmonitor.repository;

import com.robotmonitor.db.ConnectionProvider;
import com.robotmonitor.model.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TaskRepository {
    private static final String COLUMNS = "task_id, task_name, description, priority, task_status, robot_id";
    private final ConnectionProvider database;
    public TaskRepository(ConnectionProvider database) { this.database = Objects.requireNonNull(database); }

    public void create(Task task) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO task (task_name, description, priority, task_status, robot_id, task_id) VALUES (?, ?, ?, ?, ?, ?)")) {
            bind(s, task);
            s.executeUpdate();
        }
    }

    public List<Task> findAll() throws SQLException {
        return find("SELECT " + COLUMNS + " FROM task ORDER BY FIELD(priority, 'High', 'Medium', 'Low'), task_id", null);
    }

    public List<Task> findByRobotId(String robotId) throws SQLException {
        Objects.requireNonNull(robotId, "Use findAll to include unassigned tasks");
        return find("SELECT " + COLUMNS + " FROM task WHERE robot_id=? ORDER BY task_id", robotId);
    }

    public Optional<Task> findById(String id) throws SQLException {
        Objects.requireNonNull(id, "Task ID is required");
        return find("SELECT " + COLUMNS + " FROM task WHERE task_id=?", id).stream().findFirst();
    }

    /** Updates assignment and status together. Does not change the robot's status. */
    public boolean update(Task task) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE task SET task_name=?, description=?, priority=?, task_status=?, robot_id=? WHERE task_id=?")) {
            bind(s, task);
            return s.executeUpdate() == 1;
        }
    }

    public boolean delete(String id) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement("DELETE FROM task WHERE task_id=?")) {
            s.setString(1, id);
            return s.executeUpdate() == 1;
        }
    }

    private List<Task> find(String sql, String id) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(sql)) {
            if (id != null) s.setString(1, id);
            try (ResultSet rows = s.executeQuery()) {
                List<Task> tasks = new ArrayList<>();
                while (rows.next()) tasks.add(new Task(rows.getString("task_id"), rows.getString("task_name"),
                        rows.getString("description"), Priority.fromDatabase(rows.getString("priority")),
                        TaskStatus.fromDatabase(rows.getString("task_status")), rows.getString("robot_id")));
                return List.copyOf(tasks);
            }
        }
    }

    private static void bind(PreparedStatement s, Task task) throws SQLException {
        s.setString(1, task.taskName());
        s.setString(2, task.description());
        s.setString(3, task.priority().databaseValue());
        s.setString(4, task.taskStatus().databaseValue());
        if (task.robotId() == null) s.setNull(5, Types.VARCHAR);
        else s.setString(5, task.robotId());
        s.setString(6, task.taskId());
    }
}

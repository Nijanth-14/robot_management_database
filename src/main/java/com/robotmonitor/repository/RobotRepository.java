package com.robotmonitor.repository;

import com.robotmonitor.db.ConnectionProvider;
import com.robotmonitor.model.Robot;
import com.robotmonitor.model.RobotStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RobotRepository {
    private static final String COLUMNS = "robot_id, robot_name, robot_type, location, status, battery_level";
    private final ConnectionProvider database;
    public RobotRepository(ConnectionProvider database) { this.database = Objects.requireNonNull(database); }

    public void create(Robot robot) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO robot (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?)")) {
            s.setString(1, robot.robotId());
            s.setString(2, robot.robotName());
            s.setString(3, robot.robotType());
            s.setString(4, robot.location());
            s.setString(5, robot.status().databaseValue());
            s.setInt(6, robot.batteryLevel());
            s.executeUpdate();
        }
    }

    public List<Robot> findAll() throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM robot ORDER BY robot_id"); ResultSet rows = s.executeQuery()) {
            List<Robot> robots = new ArrayList<>();
            while (rows.next()) robots.add(read(rows));
            return List.copyOf(robots);
        }
    }

    public Optional<Robot> findById(String id) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM robot WHERE robot_id = ?")) {
            s.setString(1, id);
            try (ResultSet rows = s.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        }
    }

    /** Returns false when the ID does not exist. The ID itself is immutable. */
    public boolean update(Robot robot) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE robot SET robot_name=?, robot_type=?, location=?, status=?, battery_level=? WHERE robot_id=?")) {
            s.setString(1, robot.robotName());
            s.setString(2, robot.robotType());
            s.setString(3, robot.location());
            s.setString(4, robot.status().databaseValue());
            s.setInt(5, robot.batteryLevel());
            s.setString(6, robot.robotId());
            return s.executeUpdate() == 1;
        }
    }

    /** MySQL rejects deletion while tasks still reference this robot. */
    public boolean delete(String id) throws SQLException {
        try (Connection c = database.open(); PreparedStatement s = c.prepareStatement("DELETE FROM robot WHERE robot_id=?")) {
            s.setString(1, id);
            return s.executeUpdate() == 1;
        }
    }

    private static Robot read(ResultSet row) throws SQLException {
        return new Robot(row.getString("robot_id"), row.getString("robot_name"),
                row.getString("robot_type"), row.getString("location"),
                RobotStatus.fromDatabase(row.getString("status")), row.getInt("battery_level"));
    }
}

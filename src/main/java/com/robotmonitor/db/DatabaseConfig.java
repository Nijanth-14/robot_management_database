package com.robotmonitor.db;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** Deliberately not a record: generated toString() must never expose a password. */
public final class DatabaseConfig {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseConfig(String url, String user, String password) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("DB_URL must be a MySQL JDBC URL");
        }
        if (user == null || user.isBlank()) throw new IllegalArgumentException("DB_USER is required");
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static DatabaseConfig load(Path file, Map<String, String> environment) throws IOException {
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        return new DatabaseConfig(
            environment.getOrDefault("DB_URL", properties.getProperty("db.url", "jdbc:mysql://localhost:3306/robot_monitoring")),
            environment.getOrDefault("DB_USER", properties.getProperty("db.user", "")),
            environment.containsKey("DB_PASSWORD") ? environment.get("DB_PASSWORD") : properties.getProperty("db.password")
        );
    }

    public String url() { return url; }
    public String user() { return user; }
    public String password() { return password; }
    public DatabaseConfig withPassword(String value) { return new DatabaseConfig(url, user, value); }
    @Override public String toString() { return "DatabaseConfig[credentials hidden]"; }
}

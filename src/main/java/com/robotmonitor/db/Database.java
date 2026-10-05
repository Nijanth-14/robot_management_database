package com.robotmonitor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

public final class Database implements ConnectionProvider {
    private final DatabaseConfig config;
    public Database(DatabaseConfig config) { this.config = Objects.requireNonNull(config); }

    @Override public Connection open() throws SQLException {
        if (config.password() == null) {
            throw new IllegalStateException("Set DB_PASSWORD or use the interactive console prompt");
        }
        Properties properties = new Properties();
        properties.setProperty("user", config.user());
        properties.setProperty("password", config.password());
        properties.setProperty("connectTimeout", "5000");
        properties.setProperty("socketTimeout", "10000");
        // Require encryption; do not enable allowPublicKeyRetrieval or disable SSL.
        properties.setProperty("sslMode", "REQUIRED");
        return DriverManager.getConnection(config.url(), properties);
    }
}

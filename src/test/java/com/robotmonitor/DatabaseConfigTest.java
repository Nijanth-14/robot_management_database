package com.robotmonitor;

import com.robotmonitor.db.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseConfigTest {
    @TempDir Path directory;

    @Test void defaultsRequirePasswordPrompt() throws Exception {
        DatabaseConfig c = DatabaseConfig.load(directory.resolve("missing"), Map.of());
        assertEquals("jdbc:mysql://localhost:3306/robot_monitoring", c.url());
        assertEquals("root", c.user());
        assertNull(c.password());
    }

    @Test void environmentOverridesFileAndPreservesPasswordWhitespace() throws Exception {
        Path file = directory.resolve("database.properties");
        Files.writeString(file, "db.user=file-user\ndb.password=file-password\n");
        DatabaseConfig c = DatabaseConfig.load(file, Map.of("DB_USER", "env-user", "DB_PASSWORD", " secret "));
        assertEquals("env-user", c.user());
        assertEquals(" secret ", c.password());
        assertFalse(c.toString().contains("secret"));
    }

    @Test void acceptsExplicitEmptyPasswordForIsolatedTests() throws Exception {
        assertEquals("", DatabaseConfig.load(directory.resolve("missing"), Map.of("DB_PASSWORD", "")).password());
    }
}

package com.robotmonitor;

import com.robotmonitor.db.*;
import com.robotmonitor.model.*;
import com.robotmonitor.repository.*;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/** Read-only connection check. Never creates, updates or deletes records. */
public final class ConsoleApp {
    private ConsoleApp() {}

    public static void main(String[] args) {
        try {
            DatabaseConfig config = DatabaseConfig.load(Path.of("config", "database.properties"), System.getenv());
            if (config.password() == null) {
                Console console = System.console();
                if (console == null) {
                    throw new IllegalStateException("Run run.cmd in a terminal for the password prompt, or set DB_PASSWORD locally.");
                }
                char[] password = console.readPassword("MySQL password for %s: ", config.user());
                if (password == null) throw new IllegalStateException("Password entry cancelled.");
                try { config = config.withPassword(new String(password)); }
                finally { Arrays.fill(password, '\0'); }
            }
            Database database = new Database(config);
            List<Robot> robots = new RobotRepository(database).findAll();
            List<Task> tasks = new TaskRepository(database).findAll();
            System.out.println("Connected to MySQL. Database read check passed.");
            System.out.printf("%nRobots (%d)%n", robots.size());
            for (Robot r : robots) {
                System.out.printf("%s | %s | %s | %s | %d%%%n", r.robotId(), r.robotName(), r.location(), r.status(), r.batteryLevel());
            }
            System.out.printf("%nTasks (%d)%n", tasks.size());
            for (Task t : tasks) {
                System.out.printf("%s | %s | %s | %s | Robot: %s%n", t.taskId(), t.taskName(), t.priority(),
                        t.taskStatus(), t.robotId() == null ? "Unassigned" : t.robotId());
            }
        } catch (SQLException e) {
            System.err.printf("Database check failed (SQL state %s, error %d).%n", e.getSQLState(), e.getErrorCode());
            System.err.println("Check that MySQL is running, credentials are correct, and schema.sql has been applied. See README.md.");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Cannot read config/database.properties. Check file permissions and encoding.");
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

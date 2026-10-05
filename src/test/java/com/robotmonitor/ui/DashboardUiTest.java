package com.robotmonitor.ui;

import com.robotmonitor.model.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.imageio.ImageIO;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_UI_TESTS", matches = "true")
class DashboardUiTest {
    private Stage stage;
    private MonitorApp app;

    @Test @Timeout(90) void loginAndCrudThroughDesktopControls() throws Exception {
        String url = System.getenv("DB_TEST_URL");
        assertNotNull(url);
        assertTrue(url.endsWith("/robot_monitoring_test"), "Use the isolated test database");
        Platform.startup(() -> Platform.setImplicitExit(false));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String robotId = "UIR" + suffix, taskId = "UIT" + suffix;
        try {
            fx(() -> {
                stage = new Stage(); app = new MonitorApp(); app.start(stage);
                field("db-url").setText(url); field("db-user").setText("root"); field("db-password").setText("");
                button("connect").fire();
            });
            await(() -> stage.getScene().getRoot() instanceof Dashboard);
            fx(() -> {
                field("robot-id").setText(robotId); field("robot-name").setText("Demo transport");
                field("robot-type").setText("Transport"); field("robot-location").setText("Zone D");
                field("robot-battery").setText("101"); button("robot-add").fire();
                assertTrue(message().contains("between 0 and 100"));
                field("robot-battery").setText("85"); button("robot-add").fire();
            });
            await(() -> message().equals("Robot " + robotId + " saved"));
            fx(() -> {
                TableView<Robot> table = table("robot-table");
                table.getSelectionModel().select(table.getItems().stream().filter(r -> r.robotId().equals(robotId)).findFirst().orElseThrow());
                assertTrue(field("robot-id").isDisable());
                field("robot-battery").setText("18"); button("robot-update").fire();
            });
            await(() -> message().equals("Robot " + robotId + " saved") && !field("robot-id").isDisable());
            fx(() -> {
                field("robot-search").setText(robotId);
                TableView<Robot> table = table("robot-table");
                assertEquals(1, table.getItems().size()); assertEquals(18, table.getItems().get(0).batteryLevel());
                field("robot-search").clear(); snapshot("ui-robots.png");
                ((TabPane) stage.getScene().lookup("#main-tabs")).getSelectionModel().select(1);
                stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
                field("task-id").setText(taskId); field("task-name").setText("Deliver test supplies");
                combo("task-status").setValue(TaskStatus.IN_PROGRESS); button("task-add").fire();
                assertTrue(message().contains("assigned robot"));
                combo("task-robot").setValue(robotId); button("task-add").fire();
            });
            await(() -> message().equals("Task " + taskId + " saved"));
            fx(() -> {
                TableView<Task> table = table("task-table");
                Task task = table.getItems().stream().filter(t -> t.taskId().equals(taskId)).findFirst().orElseThrow();
                assertEquals(robotId, task.robotId());
                table.getSelectionModel().select(task); combo("task-status").setValue(TaskStatus.COMPLETED); button("task-update").fire();
            });
            await(() -> message().equals("Task " + taskId + " saved") && !field("task-id").isDisable());
            fx(() -> {
                snapshot("ui-tasks.png");
                TableView<Task> table = table("task-table");
                table.getSelectionModel().select(table.getItems().stream().filter(t -> t.taskId().equals(taskId)).findFirst().orElseThrow());
                confirmNext(); button("task-delete").fire();
            });
            await(() -> message().equals("Task deleted"));
            fx(() -> {
                ((TabPane) stage.getScene().lookup("#main-tabs")).getSelectionModel().select(0);
                TableView<Robot> table = table("robot-table");
                table.getSelectionModel().select(table.getItems().stream().filter(r -> r.robotId().equals(robotId)).findFirst().orElseThrow());
                confirmNext(); button("robot-delete").fire();
            });
            await(() -> message().equals("Robot deleted"));
        } finally {
            fx(() -> { if (app != null) app.stop(); if (stage != null) stage.close(); });
            Platform.exit();
        }
    }

    private void confirmNext() {
        Platform.runLater(() -> {
            for (Window window : new ArrayList<>(Window.getWindows())) {
                if (window == stage || window.getScene() == null) continue;
                if (window.getScene().getRoot() instanceof DialogPane pane) {
                    ((Button) pane.lookupButton(ButtonType.OK)).fire();
                }
            }
        });
    }
    private void snapshot(String file) {
        stage.getScene().getRoot().applyCss(); stage.getScene().getRoot().layout();
        try { ImageIO.write(SwingFXUtils.fromFXImage(stage.getScene().snapshot(null), null), "png", Path.of("target", file).toFile()); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
    private TextField field(String id) { return (TextField) stage.getScene().lookup("#" + id); }
    private Button button(String id) { return (Button) stage.getScene().lookup("#" + id); }
    @SuppressWarnings("unchecked") private <T> TableView<T> table(String id) { return (TableView<T>) stage.getScene().lookup("#" + id); }
    @SuppressWarnings("unchecked") private ComboBox<Object> combo(String id) { return (ComboBox<Object>) stage.getScene().lookup("#" + id); }
    private String message() { return ((Label) stage.getScene().lookup("#operation-status")).getText(); }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20);
        while (System.nanoTime() < deadline) {
            FutureTask<Boolean> result = new FutureTask<>(condition::getAsBoolean); Platform.runLater(result);
            if (result.get(5, TimeUnit.SECONDS)) return;
            Thread.sleep(80);
        }
        fx(() -> fail("Timed out. Current UI message: " + message()));
    }
    private void fx(Runnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(action, null); Platform.runLater(task); task.get(30, TimeUnit.SECONDS);
    }
}

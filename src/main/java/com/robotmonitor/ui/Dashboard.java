package com.robotmonitor.ui;

import com.robotmonitor.db.ConnectionProvider;
import com.robotmonitor.model.Robot;
import com.robotmonitor.model.RobotStatus;
import com.robotmonitor.model.Task;
import com.robotmonitor.model.TaskStatus;
import com.robotmonitor.repository.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;

public final class Dashboard extends BorderPane {
    public record Data(List<Robot> robots, List<Task> tasks) {}
    @FunctionalInterface interface Operation { void run() throws Exception; }
    private final ConnectionProvider db;
    private final ExecutorService worker;
    private final RobotPane robots;
    private final TaskPane tasks;
    private final Label fleet = new Label(), available = new Label(), battery = new Label(), active = new Label();
    private final Label status = new Label("Ready"), refreshed = new Label();
    private final VBox content;
    private final Timeline timer;
    private boolean busy;

    public Dashboard(ConnectionProvider db, ExecutorService worker) {
        this.db = db; this.worker = worker;
        getStyleClass().add("dashboard"); setId("dashboard");
        RobotRepository robotRepository = new RobotRepository(db);
        TaskRepository taskRepository = new TaskRepository(db);
        robots = new RobotPane(this, robotRepository);
        tasks = new TaskPane(this, taskRepository);
        Label heading = Ui.label("Robot Monitor", "title");
        Label subtitle = Ui.label("Fleet overview and task management", "muted");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refresh = Ui.button("Refresh", "refresh", this::refresh);
        CheckBox auto = new CheckBox("Refresh every 10s"); auto.setId("auto-refresh");
        HBox header = new HBox(18, new VBox(3, heading, subtitle), spacer, auto, refresh);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        HBox metrics = new HBox(14, metric("TOTAL ROBOTS", fleet), metric("AVAILABLE", available),
                metric("LOW BATTERY · ≤ 20%", battery), metric("ACTIVE TASKS", active));
        TabPane tabs = new TabPane(new Tab("Robots", robots), new Tab("Tasks", tasks));
        tabs.setId("main-tabs"); tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        content = new VBox(20, header, metrics, tabs); content.setPadding(new Insets(25)); setCenter(content);
        status.setId("operation-status"); status.setWrapText(true);
        Region footerSpace = new Region(); HBox.setHgrow(footerSpace, Priority.ALWAYS);
        HBox footer = new HBox(12, status, footerSpace, refreshed); footer.getStyleClass().add("footer"); setBottom(footer);
        timer = new Timeline(new KeyFrame(Duration.seconds(10), e -> { if (!busy) refresh(); }));
        timer.setCycleCount(Timeline.INDEFINITE);
        auto.selectedProperty().addListener((o, old, enabled) -> { if (enabled) timer.play(); else timer.stop(); });
    }

    private VBox metric(String title, Label value) {
        value.getStyleClass().add("metric-number");
        VBox box = new VBox(6, Ui.label(title, "eyebrow"), value);
        box.getStyleClass().add("metric"); HBox.setHgrow(box, Priority.ALWAYS); box.setMaxWidth(Double.MAX_VALUE); return box;
    }

    public static Data load(ConnectionProvider db) throws Exception {
        return new Data(new RobotRepository(db).findAll(), new TaskRepository(db).findAll());
    }

    public void accept(Data data) {
        robots.setData(data.robots(), data.tasks()); tasks.setData(data.tasks(), data.robots());
        fleet.setText(String.valueOf(data.robots().size()));
        available.setText(String.valueOf(data.robots().stream().filter(r -> r.status() == RobotStatus.AVAILABLE).count()));
        battery.setText(String.valueOf(data.robots().stream().filter(r -> r.batteryLevel() <= 20).count()));
        active.setText(String.valueOf(data.tasks().stream().filter(t -> t.taskStatus() == TaskStatus.IN_PROGRESS).count()));
        refreshed.setText("Last refreshed " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    void refresh() { submit("Refreshing…", "Records refreshed", () -> {}, () -> {}); }

    void submit(String pending, String success, Operation change, Runnable afterChange) {
        if (busy) return;
        busy = true; content.setDisable(true); status.getStyleClass().remove("error"); status.setText(pending);
        worker.submit(() -> {
            boolean saved = false;
            try {
                change.run(); saved = true;
                Data data = load(db);
                Platform.runLater(() -> {
                    afterChange.run(); accept(data); finish(success, false);
                });
            } catch (Exception e) {
                boolean mutationCompleted = saved;
                Platform.runLater(() -> {
                    if (mutationCompleted) afterChange.run();
                    finish(mutationCompleted ? "Operation completed, but refresh failed. Click Refresh. " + Ui.message(e) : Ui.message(e), true);
                });
            }
        });
    }

    private void finish(String message, boolean error) {
        busy = false; content.setDisable(false); status.setText(message);
        if (error && !status.getStyleClass().contains("error")) status.getStyleClass().add("error");
    }
    void invalid(Exception e) { finish(Ui.message(e), true); }
    boolean confirm(String record) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete " + record + "? This permanently removes the record.", ButtonType.CANCEL, ButtonType.OK);
        alert.initOwner(getScene().getWindow()); alert.setTitle("Confirm deletion"); alert.setHeaderText("Delete record");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    static void requireFound(boolean found) {
        if (!found) throw new IllegalStateException("This record no longer exists. Refresh the table.");
    }
    public void close() { timer.stop(); }
}

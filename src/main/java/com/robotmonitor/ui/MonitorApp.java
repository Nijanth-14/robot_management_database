package com.robotmonitor.ui;

import com.robotmonitor.db.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MonitorApp extends Application {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "database-worker"); thread.setDaemon(true); return thread;
    });
    private Dashboard dashboard;

    @Override public void start(Stage stage) {
        stage.setTitle("Robot Monitor | Fleet Operations");
        TextField url = Ui.field("jdbc:mysql://localhost:3306/robot_monitoring", "db-url");
        TextField user = Ui.field("root", "db-user");
        PasswordField password = new PasswordField(); password.setId("db-password");
        password.setPromptText("Your MySQL password");
        Label error = Ui.label("", "error"); error.setWrapText(true); error.setId("login-error");
        try {
            DatabaseConfig config = DatabaseConfig.load(Path.of("config", "database.properties"), System.getenv());
            url.setText(config.url()); user.setText(config.user());
            if (config.password() != null) password.setText(config.password());
        } catch (Exception e) {
            url.setText("jdbc:mysql://localhost:3306/robot_monitoring"); user.setText("root");
            error.setText("Local configuration could not be loaded. Enter your connection details below.");
        }
        Button connect = new Button("Connect to database"); connect.setId("connect");
        connect.getStyleClass().add("primary"); connect.setDefaultButton(true); connect.setMaxWidth(Double.MAX_VALUE);
        VBox form = new VBox(16, Ui.label("FLEET OPERATIONS", "eyebrow"),
                Ui.label("Robot Monitor", "title"),
                Ui.label("Connect to manage your robots and their tasks.", "muted"),
                Ui.input("Database URL", url), Ui.input("Username", user), Ui.input("Password", password),
                connect, error, Ui.label("Your password stays in memory for this session.", "muted"));
        form.setMaxWidth(470); form.getStyleClass().add("login-card");
        StackPane login = new StackPane(form); login.getStyleClass().add("login-root");
        StackPane.setAlignment(form, Pos.CENTER);
        Scene scene = new Scene(login, 1180, 790);
        scene.getStylesheets().add(MonitorApp.class.getResource("monitor.css").toExternalForm());
        stage.setScene(scene); stage.setMinWidth(1000); stage.setMinHeight(700);
        connect.setOnAction(event -> {
            Database db;
            try { db = new Database(new DatabaseConfig(url.getText().trim(), user.getText().trim(), password.getText())); }
            catch (RuntimeException e) { error.setText(Ui.message(e)); return; }
            form.setDisable(true); error.setText("Connecting…");
            worker.submit(() -> {
                try {
                    Dashboard.Data data = Dashboard.load(db);
                    Platform.runLater(() -> {
                        password.clear();
                        dashboard = new Dashboard(db, worker);
                        dashboard.accept(data);
                        scene.setRoot(dashboard);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> { form.setDisable(false); error.setText(Ui.message(e)); });
                }
            });
        });
        stage.show();
    }

    @Override public void stop() {
        if (dashboard != null) dashboard.close();
        worker.shutdownNow();
    }
}

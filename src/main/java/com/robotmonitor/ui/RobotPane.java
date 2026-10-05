package com.robotmonitor.ui;

import com.robotmonitor.model.*;
import com.robotmonitor.repository.RobotRepository;
import com.robotmonitor.service.RobotSearch;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.stream.Collectors;

final class RobotPane extends BorderPane {
    private final Dashboard dashboard;
    private final RobotRepository repository;
    private final TableView<Robot> table = new TableView<>();
    private final TextField search = Ui.field("Search ID, name, type or location", "robot-search");
    private final ComboBox<String> filter = Ui.combo(new String[]{"All statuses", "Available", "Busy", "Charging", "Offline", "Maintenance"}, "robot-filter");
    private final TextField id = Ui.field("R004", "robot-id"), name = Ui.field("Robot name", "robot-name"),
            type = Ui.field("Transport, inspection…", "robot-type"), location = Ui.field("Zone A", "robot-location"),
            battery = Ui.field("0–100", "robot-battery");
    private final ComboBox<RobotStatus> status = Ui.combo(RobotStatus.values(), "robot-status");
    private final Label count = Ui.label("", "muted"), mode = Ui.label("New robot", "section-title");
    private List<Robot> data = List.of();
    private List<Task> tasks = List.of();
    private boolean loading;

    RobotPane(Dashboard dashboard, RobotRepository repository) {
        this.dashboard = dashboard; this.repository = repository; getStyleClass().add("workspace");
        table.setId("robot-table"); table.setPlaceholder(new Label("No robots match. Add a robot or clear the filters."));
        table.getColumns().add(Ui.column("ID", 70, Robot::robotId));
        table.getColumns().add(Ui.column("Name", 110, Robot::robotName));
        table.getColumns().add(Ui.column("Type", 95, Robot::robotType));
        table.getColumns().add(Ui.column("Location", 90, Robot::location));
        table.getColumns().add(Ui.column("Status", 90, Robot::status));
        table.getColumns().add(Ui.column("Battery %", 85, Robot::batteryLevel));
        table.getColumns().add(Ui.column("Open tasks", 130, r -> tasks.stream()
                .filter(t -> r.robotId().equalsIgnoreCase(t.robotId() == null ? "" : t.robotId()))
                .filter(t -> t.taskStatus() == TaskStatus.PENDING || t.taskStatus() == TaskStatus.IN_PROGRESS)
                .map(Task::taskName).collect(Collectors.joining(", "))));
        table.getSelectionModel().selectedItemProperty().addListener((o, old, robot) -> {
            if (robot != null && !loading) edit(robot);
        });
        HBox searchRow = new HBox(10, search, filter); HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        VBox list = new VBox(10, searchRow, table, count); VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        setCenter(list);
        Button add = Ui.button("Add robot", "robot-add", () -> save(false)); add.getStyleClass().add("primary");
        Button update = Ui.button("Save changes", "robot-update", () -> save(true));
        Button delete = Ui.button("Delete", "robot-delete", this::delete); delete.getStyleClass().add("danger");
        Button clear = Ui.button("Clear / New", "robot-clear", this::clear);
        add.disableProperty().bind(id.disableProperty());
        update.disableProperty().bind(id.disableProperty().not()); delete.disableProperty().bind(id.disableProperty().not());
        Label help = Ui.label("Select a row to edit. Use Clear / New to register another robot.", "muted"); help.setWrapText(true);
        VBox form = new VBox(11, mode, help, Ui.input("Robot ID", id), Ui.input("Name", name), Ui.input("Type", type),
                Ui.input("Location", location), Ui.input("Status", status), Ui.input("Battery level (%)", battery));
        form.getStyleClass().add("editor");
        ScrollPane scroll = Ui.scroll(form); VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        VBox actions = new VBox(8, new HBox(8, add, update), new HBox(8, clear, delete)); actions.getStyleClass().add("editor-actions");
        VBox editor = new VBox(scroll, actions); editor.setPrefWidth(310); editor.setMinWidth(310);
        setRight(editor); BorderPane.setMargin(editor, new javafx.geometry.Insets(0, 0, 0, 18));
        search.textProperty().addListener((o, a, b) -> applyFilter());
        filter.valueProperty().addListener((o, a, b) -> applyFilter());
        clear();
    }

    void setData(List<Robot> robots, List<Task> tasks) { data = robots; this.tasks = tasks; applyFilter(); }
    private void applyFilter() {
        loading = true;
        String chosen = filter.getValue();
        List<Robot> filtered = RobotSearch.filter(data, search.getText(),
                chosen == null || chosen.equals("All statuses") ? null : RobotStatus.fromDatabase(chosen));
        table.setItems(FXCollections.observableArrayList(filtered)); table.sort();
        if (id.isDisable()) filtered.stream().filter(r -> r.robotId().equals(id.getText())).findFirst().ifPresent(r -> table.getSelectionModel().select(r));
        count.setText(filtered.size() + " of " + data.size() + " robots · Click column headings to sort");
        loading = false;
    }
    private void edit(Robot r) {
        id.setText(r.robotId()); id.setDisable(true); name.setText(r.robotName()); type.setText(r.robotType());
        location.setText(r.location()); status.setValue(r.status()); battery.setText(String.valueOf(r.batteryLevel()));
        mode.setText("Edit " + r.robotId());
    }
    private void clear() {
        table.getSelectionModel().clearSelection(); id.setDisable(false); id.clear(); name.clear(); type.clear(); location.clear();
        status.setValue(RobotStatus.AVAILABLE); battery.setText("100"); mode.setText("New robot");
    }
    private void save(boolean update) {
        try {
            int level;
            try { level = Integer.parseInt(battery.getText().trim()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Battery level must be a whole number from 0 to 100."); }
            Robot r = new Robot(id.getText(), name.getText(), type.getText(), location.getText(), status.getValue(), level);
            dashboard.submit("Saving robot…", "Robot " + r.robotId() + " saved", () -> {
                if (update) Dashboard.requireFound(repository.update(r)); else repository.create(r);
            }, this::clear);
        } catch (RuntimeException e) { dashboard.invalid(e); }
    }
    private void delete() {
        String robotId = id.getText();
        if (dashboard.confirm("robot " + robotId)) dashboard.submit("Deleting robot…", "Robot deleted",
                () -> Dashboard.requireFound(repository.delete(robotId)), this::clear);
    }
}

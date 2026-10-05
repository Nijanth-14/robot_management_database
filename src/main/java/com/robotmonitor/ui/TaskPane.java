package com.robotmonitor.ui;

import com.robotmonitor.model.*;
import com.robotmonitor.repository.TaskRepository;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.StringConverter;
import java.util.List;
import java.util.Locale;

final class TaskPane extends BorderPane {
    private final Dashboard dashboard;
    private final TaskRepository repository;
    private final TableView<Task> table = new TableView<>();
    private final TextField search = Ui.field("Search task ID, name or robot", "task-search");
    private final ComboBox<String> filter = Ui.combo(new String[]{"All statuses", "Pending", "In Progress", "Completed", "Cancelled"}, "task-filter");
    private final TextField id = Ui.field("T004", "task-id"), name = Ui.field("Task name", "task-name");
    private final TextArea description = new TextArea();
    private final ComboBox<com.robotmonitor.model.Priority> priority = Ui.combo(com.robotmonitor.model.Priority.values(), "task-priority");
    private final ComboBox<TaskStatus> status = Ui.combo(TaskStatus.values(), "task-status");
    private final ComboBox<String> robot = new ComboBox<>();
    private final Label count = Ui.label("", "muted"), mode = Ui.label("New task", "section-title");
    private List<Task> data = List.of();
    private List<Robot> robots = List.of();
    private boolean loading;

    TaskPane(Dashboard dashboard, TaskRepository repository) {
        this.dashboard = dashboard; this.repository = repository; getStyleClass().add("workspace");
        description.setId("task-description"); description.setPrefRowCount(2); description.setWrapText(true);
        table.setId("task-table"); table.setPlaceholder(new Label("No tasks match. Create a task or clear the filters."));
        table.getColumns().add(Ui.column("ID", 75, Task::taskId));
        table.getColumns().add(Ui.column("Task", 180, Task::taskName));
        table.getColumns().add(Ui.column("Priority", 85, Task::priority));
        table.getColumns().add(Ui.column("Status", 110, Task::taskStatus));
        table.getColumns().add(Ui.column("Assigned robot", 190, t -> robotLabel(t.robotId())));
        table.getSelectionModel().selectedItemProperty().addListener((o, old, task) -> { if (task != null && !loading) edit(task); });
        HBox searchRow = new HBox(10, search, filter); HBox.setHgrow(search, javafx.scene.layout.Priority.ALWAYS);
        VBox list = new VBox(10, searchRow, table, count); VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS); setCenter(list);
        robot.setId("task-robot"); robot.setMaxWidth(Double.MAX_VALUE);
        robot.setConverter(new StringConverter<>() {
            @Override public String toString(String value) { return robotLabel(value); }
            @Override public String fromString(String value) { return value; }
        });
        Button add = Ui.button("Add task", "task-add", () -> save(false)); add.getStyleClass().add("primary");
        Button update = Ui.button("Save changes", "task-update", () -> save(true));
        Button delete = Ui.button("Delete", "task-delete", this::delete); delete.getStyleClass().add("danger");
        Button clear = Ui.button("Clear / New", "task-clear", this::clear);
        add.disableProperty().bind(id.disableProperty());
        update.disableProperty().bind(id.disableProperty().not()); delete.disableProperty().bind(id.disableProperty().not());
        Label help = Ui.label("Assign a robot before starting a task. Robot status is updated separately on the Robots tab.", "muted"); help.setWrapText(true);
        VBox form = new VBox(10, mode, help, Ui.input("Task ID", id), Ui.input("Name", name), Ui.input("Description", description),
                Ui.input("Priority", priority), Ui.input("Task status", status), Ui.input("Assigned robot", robot));
        form.getStyleClass().add("editor"); ScrollPane scroll = Ui.scroll(form); VBox.setVgrow(scroll, javafx.scene.layout.Priority.ALWAYS);
        VBox actions = new VBox(8, new HBox(8, add, update), new HBox(8, clear, delete)); actions.getStyleClass().add("editor-actions");
        VBox editor = new VBox(scroll, actions); editor.setPrefWidth(310); editor.setMinWidth(310);
        setRight(editor); BorderPane.setMargin(editor, new javafx.geometry.Insets(0, 0, 0, 18));
        search.textProperty().addListener((o, a, b) -> applyFilter()); filter.valueProperty().addListener((o, a, b) -> applyFilter());
        clear();
    }

    void setData(List<Task> tasks, List<Robot> robots) {
        data = tasks; this.robots = robots;
        String previous = robot.getValue();
        robot.getItems().clear(); robot.getItems().add(""); robot.getItems().addAll(robots.stream().map(Robot::robotId).toList());
        if (previous != null && !previous.isEmpty() && !robot.getItems().contains(previous)) robot.getItems().add(previous);
        robot.setValue(previous == null ? "" : previous); applyFilter();
    }
    private String robotLabel(String value) {
        if (value == null || value.isEmpty()) return "Unassigned";
        return robots.stream().filter(r -> r.robotId().equalsIgnoreCase(value)).findFirst()
                .map(r -> r.robotId() + " · " + r.robotName()).orElse(value);
    }
    private void applyFilter() {
        loading = true;
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<Task> filtered = data.stream()
                .filter(t -> filter.getValue() == null || filter.getValue().equals("All statuses") || t.taskStatus().databaseValue().equals(filter.getValue()))
                .filter(t -> t.taskId().toLowerCase(Locale.ROOT).contains(query) || t.taskName().toLowerCase(Locale.ROOT).contains(query)
                        || robotLabel(t.robotId()).toLowerCase(Locale.ROOT).contains(query)).toList();
        table.setItems(FXCollections.observableArrayList(filtered)); table.sort();
        if (id.isDisable()) filtered.stream().filter(t -> t.taskId().equals(id.getText())).findFirst().ifPresent(t -> table.getSelectionModel().select(t));
        count.setText(filtered.size() + " of " + data.size() + " tasks · Click column headings to sort"); loading = false;
    }
    private void edit(Task t) {
        id.setText(t.taskId()); id.setDisable(true); name.setText(t.taskName()); description.setText(t.description());
        priority.setValue(t.priority()); status.setValue(t.taskStatus()); robot.setValue(t.robotId() == null ? "" : t.robotId());
        mode.setText("Edit " + t.taskId());
    }
    private void clear() {
        table.getSelectionModel().clearSelection(); id.setDisable(false); id.clear(); name.clear(); description.clear();
        priority.setValue(com.robotmonitor.model.Priority.MEDIUM); status.setValue(TaskStatus.PENDING); robot.setValue(""); mode.setText("New task");
    }
    private void save(boolean update) {
        try {
            String assigned = robot.getValue();
            Task t = new Task(id.getText(), name.getText(), description.getText(), priority.getValue(), status.getValue(),
                    assigned == null || assigned.isEmpty() ? null : assigned);
            dashboard.submit("Saving task…", "Task " + t.taskId() + " saved", () -> {
                if (update) Dashboard.requireFound(repository.update(t)); else repository.create(t);
            }, this::clear);
        } catch (RuntimeException e) { dashboard.invalid(e); }
    }
    private void delete() {
        String taskId = id.getText();
        if (dashboard.confirm("task " + taskId)) dashboard.submit("Deleting task…", "Task deleted",
                () -> Dashboard.requireFound(repository.delete(taskId)), this::clear);
    }
}

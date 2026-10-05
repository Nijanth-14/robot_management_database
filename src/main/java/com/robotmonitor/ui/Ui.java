package com.robotmonitor.ui;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.sql.SQLException;
import java.util.function.Function;

final class Ui {
    private Ui() {}
    static Label label(String text, String style) {
        Label label = new Label(text); label.getStyleClass().add(style); return label;
    }
    static Button button(String text, String id, Runnable action) {
        Button b = new Button(text); b.setId(id); b.setOnAction(e -> action.run()); return b;
    }
    static TextField field(String prompt, String id) {
        TextField f = new TextField(); f.setPromptText(prompt); f.setId(id); return f;
    }
    static <T> ComboBox<T> combo(T[] values, String id) {
        ComboBox<T> c = new ComboBox<>(); c.getItems().addAll(values); c.setId(id);
        c.setMaxWidth(Double.MAX_VALUE); c.getSelectionModel().selectFirst(); return c;
    }
    static VBox input(String label, Node field) {
        Label name = new Label(label); name.setLabelFor(field);
        return new VBox(5, name, field);
    }
    static <T,V> TableColumn<T,V> column(String title, double width, Function<T,V> read) {
        TableColumn<T,V> col = new TableColumn<>(title); col.setPrefWidth(width);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(read.apply(c.getValue())));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(V value, boolean empty) {
                super.updateItem(value, empty);
                String text = empty || value == null ? "" : value.toString();
                setText(text); setTooltip(text.isEmpty() ? null : new Tooltip(text));
            }
        });
        return col;
    }
    static ScrollPane scroll(Node content) {
        ScrollPane scroll = new ScrollPane(content); scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); return scroll;
    }
    static String message(Throwable error) {
        if (error instanceof SQLException sql) {
            return switch (sql.getErrorCode()) {
                case 1045 -> "MySQL rejected the username or password. Check your credentials.";
                case 1049 -> "Database not found. Check the database name and run schema.sql if needed.";
                case 1146 -> "A required table is missing. Complete the database setup first.";
                case 1062 -> "That ID already exists. Choose a different ID or select the existing record to update it.";
                case 1451 -> "This robot still has tasks. Reassign or remove those tasks before deleting the robot.";
                case 1452 -> "The assigned robot no longer exists. Refresh and select another robot.";
                case 3819, 1406, 1265 -> "The database rejected a value. Check the field lengths, status and battery level.";
                default -> "Database operation failed. Check that MySQL is running and the connection settings are correct. (SQL state " + sql.getSQLState() + ")";
            };
        }
        if (error instanceof IllegalArgumentException || error instanceof IllegalStateException) return error.getMessage();
        return "The operation could not be completed. Please try again.";
    }
}

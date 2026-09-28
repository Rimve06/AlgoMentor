package com.algomentor.controller;

import com.algomentor.model.Attempt;
import com.algomentor.util.AppExecutors;
import com.algomentor.util.SessionContext;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

/**
 * Demonstrates the remaining two CRUD operations (Update + Delete) that
 * MainController/LoginController don't touch: editing a note in place
 * (UPDATE) and removing a record (DELETE), both via DatabaseManager,
 * both dispatched off the FX thread onto the shared AppExecutors pool.
 */
public class HistoryController {

    @FXML private TableView<Attempt> historyTable;
    @FXML private TableColumn<Attempt, String> algorithmColumn;
    @FXML private TableColumn<Attempt, Integer> inputSizeColumn;
    @FXML private TableColumn<Attempt, Long> durationColumn;
    @FXML private TableColumn<Attempt, String> noteColumn;
    @FXML private TableColumn<Attempt, String> dateColumn;
    @FXML private Label statusLabel;
    @FXML private Button backButton;

    @FXML
    public void initialize() {
        algorithmColumn.setCellValueFactory(new PropertyValueFactory<>("algorithmName"));
        inputSizeColumn.setCellValueFactory(new PropertyValueFactory<>("inputSize"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationMillis"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // UPDATE: editable note column, committed straight to SQLite.
        noteColumn.setCellValueFactory(new PropertyValueFactory<>("note"));
        noteColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        noteColumn.setOnEditCommit(event -> {
            Attempt attempt = event.getRowValue();
            String newNote = event.getNewValue();
            attempt.setNote(newNote);
            AppExecutors.get().submit(() -> {
                try {
                    SessionContext.db().updateAttemptNote(attempt.getId(), newNote);
                } catch (Exception e) {
                    Platform.runLater(() -> statusLabel.setText("Failed to save note: " + e.getMessage()));
                }
            });
        });
        historyTable.setEditable(true);

        loadAttempts();
    }

    private void loadAttempts() {
        if (SessionContext.getCurrentUser() == null) return;
        int userId = SessionContext.getCurrentUser().getId();
        statusLabel.setText("Loading...");
        AppExecutors.get().submit(() -> {
            try {
                List<Attempt> attempts = SessionContext.db().getAttemptsForUser(userId);
                ObservableList<Attempt> items = FXCollections.observableArrayList(attempts);
                Platform.runLater(() -> {
                    historyTable.setItems(items);
                    statusLabel.setText(items.isEmpty() ? "No attempts yet - run an algorithm first." : "");
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("Failed to load: " + e.getMessage()));
            }
        });
    }

    // DELETE
    @FXML
    private void handleDelete() {
        Attempt selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a row to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete this attempt record?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                AppExecutors.get().submit(() -> {
                    try {
                        SessionContext.db().deleteAttempt(selected.getId());
                        Platform.runLater(this::loadAttempts);
                    } catch (Exception e) {
                        Platform.runLater(() -> statusLabel.setText("Delete failed: " + e.getMessage()));
                    }
                });
            }
        });
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            Scene scene = new Scene(root, stage.getScene().getWidth(), stage.getScene().getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception ignored) {}
    }
}
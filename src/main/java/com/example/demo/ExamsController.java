package com.example.demo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ExamsController {

    @FXML private TableView<Exam> examTable;
    @FXML private TableColumn<Exam, String> subjectColumn;
    @FXML private TableColumn<Exam, String> dateColumn;
    @FXML private TableColumn<Exam, String> timeColumn;
    @FXML private TableColumn<Exam, String> locationColumn;

    private final ObservableList<Exam> examList = FXCollections.observableArrayList();

    private static final String FILE_PATH = "egzaminy.txt";

    @FXML
    private void initialize() {
        subjectColumn.setCellValueFactory(new PropertyValueFactory<>("subject"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        locationColumn.setCellValueFactory(new PropertyValueFactory<>("location"));

        examTable.setItems(examList);

        loadExamsFromFile();
    }

    @FXML
    private void handleAdd() {
        Exam newExam = showExamDialog(null);
        if (newExam != null) {
            examList.add(newExam);
            saveExamsToFile();
        }
    }

    @FXML
    private void handleEdit() {
        Exam selected = examTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Exam updated = showExamDialog(selected);
            if (updated != null) {
                selected.setSubject(updated.getSubject());
                selected.setDate(updated.getDate());
                selected.setTime(updated.getTime());
                selected.setLocation(updated.getLocation());
                examTable.refresh();
                saveExamsToFile();
            }
        } else {
            showAlert("No exam has been selected for editing.");
        }
    }

    @FXML
    private void handleDelete() {
        Exam selected = examTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            examList.remove(selected);
            saveExamsToFile();
        } else {
            showAlert("No exam has been selected for deletion.");
        }
    }

    private Exam showExamDialog(Exam existingExam) {
        Dialog<Exam> dialog = new Dialog<>();
        dialog.setTitle(existingExam == null ?  "Add exam" : "Edit exam");

        ButtonType okButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButtonType, ButtonType.CANCEL);

        TextField subjectField = new TextField();
        TextField dateField = new TextField();
        TextField timeField = new TextField();
        TextField locationField = new TextField();

        if (existingExam != null) {
            subjectField.setText(existingExam.getSubject());
            dateField.setText(existingExam.getDate());
            timeField.setText(existingExam.getTime());
            locationField.setText(existingExam.getLocation());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Subject:"), 0, 0);
        grid.add(subjectField, 1, 0);
        grid.add(new Label("Date (YYYY-MM-DD):"), 0, 1);
        grid.add(dateField, 1, 1);
        grid.add(new Label("Time (HH:mm):"), 0, 2);
        grid.add(timeField, 1, 2);
        grid.add(new Label("Room:"), 0, 3);
        grid.add(locationField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        Node okButton = dialog.getDialogPane().lookupButton(okButtonType);
        okButton.addEventFilter(ActionEvent.ACTION, event -> {
            String date = dateField.getText().trim();
            String time = timeField.getText().trim();
            if (!isValidDate(date)) {
                showAlert("Invalid date format. Please use: YYYY-MM-DD");
                event.consume();
            }
            if (!isValidTime(time)) {
                showAlert("Invalid time format. Please use: HH:mm (ex. 14:00)");
                event.consume();
            }
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButtonType) {
                return new Exam(
                        subjectField.getText(),
                        dateField.getText(),
                        timeField.getText(),
                        locationField.getText()
                );
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Uwaga");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isValidDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean isValidTime(String timeStr) {
        try {
            LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void saveExamsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Exam exam : examList) {
                writer.println(String.join(",",
                        exam.getSubject(),
                        exam.getDate(),
                        exam.getTime(),
                        exam.getLocation()
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadExamsFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1); // -1 żeby uwzględnić puste pola
                if (parts.length == 4) {
                    examList.add(new Exam(parts[0], parts[1], parts[2], parts[3]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

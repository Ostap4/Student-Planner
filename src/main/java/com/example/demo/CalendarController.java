package com.example.demo;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CalendarController implements Initializable {
    private LocalDate highlightedDate = null; 
    ZonedDateTime dateFocus;
    ZonedDateTime today;
    private String currentWeekType = updateCurrentWeekType();
    private Label selectedLabel = null;
    @FXML
    private void openExamsView() {

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/demo/ExamsView.fxml"));

            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Harmonogram Egzaminów");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private Text year;
    @FXML
    private AnchorPane mainPane;

    @FXML
    private Text month;

    @FXML
    private FlowPane calendar;
    private final List<VBox> dayBoxes = new ArrayList<>();
    private final Map<LocalDate, List<String>> plansMap = new HashMap<>();
    private static final String USER_PLANS_FILE_PATH = "user_plans.txt";

    @FXML
    private TextField searchField;
    @FXML
    private ListView<String> searchResults;


    private void saveUserPlansToFile(Map<LocalDate, List<String>> plans) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_PLANS_FILE_PATH))) {
            for (Map.Entry<LocalDate, List<String>> entry : plans.entrySet()) {
                LocalDate date = entry.getKey();
                List<String> planList = entry.getValue();
                for (String plan : planList) {
                    writer.println(date.toString() + "," + plan);
                }
            }
            System.out.println("The user's plans have been successfully saved to the file."); 
        } catch (IOException e) {
            System.err.println("Error saving user plans to the file:" + e.getMessage());
           
        }
    }

    private Map<LocalDate, List<String>> loadUserPlansFromFile() {
        Map<LocalDate, List<String>> loadedPlans = new HashMap<>();
        File file = new File(USER_PLANS_FILE_PATH);
        if (!file.exists()) {
            System.out.println("The file containing the user's plans does not exist. I am creating a new one.");
            return loadedPlans; // Zwracamy pustą mapę, jeśli plik не istnieje
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2) {
                    try {
                        LocalDate date = LocalDate.parse(parts[0]);
                        String plan = parts[1];
                        loadedPlans.computeIfAbsent(date, k -> new ArrayList<>()).add(plan);
                    } catch (DateTimeParseException e) {
                        System.err.println("Date parsing error in the file:" + parts[0] + ".Skipping the entry.");
                        // Możesz logować ten błąd
                    }
                } else {
                    System.err.println("Invalid line format in the file:" + line + ". Skipping the entry.");
                    // Możesz logować ten błąd
                }
            }
            System.out.println("The user's plans have been successfully loaded from the file.");
        } catch (IOException e) {
            System.err.println("Error reading user plans from the file: " + e.getMessage());
            
        }
        return loadedPlans;
    }


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        dateFocus = ZonedDateTime.now();
        today = ZonedDateTime.now();
        initializeSchedule();
        currentWeekType=updateCurrentWeekType();
        updateWeekInfoLabel();
        drawCalendar();
        calendar.setFocusTraversable(true);
        calendar.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE && selectedLabel != null) {
                try{
                    for (VBox dayBox : dayBoxes) {
                        if (dayBox.getChildren().contains(selectedLabel)) {
                            dayBox.getChildren().remove(selectedLabel);

                            LocalDate date = (LocalDate) dayBox.getUserData();
                            if (plansMap.containsKey(date)) {
                                plansMap.get(date).remove(selectedLabel.getText());
                                if (plansMap.get(date).isEmpty()) {
                                    plansMap.remove(date);
                            }
                        }
                            System.out.println("Deleted " + selectedLabel.getText());

                        selectedLabel = null;
                        break;
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                }
            }
        });

        
        Platform.runLater(() -> calendar.requestFocus());
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateSearchResults(newValue);
        });
        searchResults.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // Перевірка на двократний клік
                String selectedItem = searchResults.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    LocalDate selectedDate = extractDateFromResult(selectedItem);
                    if (selectedDate != null) {

                        dateFocus = selectedDate.atStartOfDay(ZoneId.systemDefault());
                        highlightedDate = selectedDate;
                        drawCalendar();
                    }
                }
            }
        });
        userPlans=loadUserPlansFromFile();
    }
    @FXML
    private Button clearSearchButton;

    @FXML
    private void clearSearchResults(ActionEvent event) {
        highlightedDate = null;
        searchResults.getItems().clear();
        drawCalendar();
    }

    private LocalDate getNextDateForDay(DayOfWeek targetDay) {
        LocalDate today = LocalDate.now();
        int daysUntilTarget = (targetDay.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        if (daysUntilTarget == 0) daysUntilTarget = 7;
        return today.plusDays(daysUntilTarget);
    }
    private static final Map<DayOfWeek, String> englishDays = Map.of(
            DayOfWeek.MONDAY, "Monday",
            DayOfWeek.TUESDAY, "Tuesday",
            DayOfWeek.WEDNESDAY, "Wednesday",
            DayOfWeek.THURSDAY, "Thursday",
            DayOfWeek.FRIDAY, "Friday",
            DayOfWeek.SATURDAY, "Saturday",
            DayOfWeek.SUNDAY, "Sunday"
    );

    private static class SearchResult {
        private final LocalDate date;
        private final String text;

        public SearchResult(LocalDate date, String text) {
            this.date = date;
            this.text = text;
        }

        public LocalDate getDate() {
            return date;
        }

        public String getText() {
            return text;
        }
    }

    private LocalDate extractDateFromResult(String result) {

        String datePattern = "\\d{2}\\.\\d{2}\\.\\d{4}";
        Pattern pattern = Pattern.compile(datePattern);
        Matcher matcher = pattern.matcher(result);

        if (matcher.find()) {
            String dateString = matcher.group();
            try {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return LocalDate.parse(dateString, formatter);
            } catch (DateTimeParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }





    private void updateSearchResults(String query) {
        searchResults.getItems().clear();
        if (query == null || query.isEmpty()) {
            return;
        }

        List<SearchResult> results = new ArrayList<>();
        LocalDate today = LocalDate.now();
        int weeksAhead = 15;

        for (int i = 0; i <= weeksAhead; i++) {
            LocalDate startOfWeek = today.plusWeeks(i).with(DayOfWeek.MONDAY);
            int weekNumber = startOfWeek.get(WeekFields.of(Locale.getDefault()).weekOfYear());
            String weekType = (weekNumber % 2 == 0) ? "A" : "B";
            Map<DayOfWeek, List<String>> weekSchedule = scheduleByWeek.get(weekType);

            if (weekSchedule == null) continue;

            for (DayOfWeek day : DayOfWeek.values()) {
                List<String> plans = weekSchedule.getOrDefault(day, new ArrayList<>());

                for (String plan : plans) {
                    if (plan.toLowerCase().contains(query.toLowerCase())) {
                        String[] parts = plan.split(",");
                        if (parts.length >= 3) {
                            String subjectName = parts[2].trim();
                            LocalDate realDate = startOfWeek.with(day);
                            if (realDate.isBefore(today)) {
                                realDate = realDate.plusWeeks(1);
                            }
                            String englishDay = englishDays.getOrDefault(day, day.toString());

                            String resultText = englishDay + " (" + realDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) + "): " + subjectName;
                            results.add(new SearchResult(realDate, resultText));
                        }
                    }
                }
            }
        }


        results.sort(Comparator.comparing(SearchResult::getDate));


        for (SearchResult result : results) {
            searchResults.getItems().add(result.getText());
        }
    }





    private Label createPlanLabel(String text, LocalDate date) {
                Label label = new Label(text);
                label.setStyle("-fx-background-color: #A0522D ; -fx-padding: 4; -fx-border-color: #FFA07A; -fx-border-width: 1;");
                label.setMaxWidth(Double.MAX_VALUE);


                label.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        selectedLabel = label;
                        highlightSelectedLabel(label);
                    }
                });


                ContextMenu contextMenu = new ContextMenu();
                MenuItem deleteItem = new MenuItem("Delete");
                deleteItem.setOnAction(e -> {
                    VBox parent = (VBox) label.getParent();
                    parent.getChildren().remove(label);
                    if (userPlans.containsKey(date)) {
                        userPlans.get(date).remove(text);
                        if (userPlans.get(date).isEmpty()) {
                            userPlans.remove(date);
                        }
                    }
                    selectedLabel = null;
                    drawCalendar();
                });
                contextMenu.getItems().add(deleteItem);

                label.setOnContextMenuRequested(e -> contextMenu.show(label, e.getScreenX(), e.getScreenY()));

                return label;
            }


            private void highlightSelectedLabel(Label label) {
                for (VBox dayBox : dayBoxes) {
                    for (javafx.scene.Node node : dayBox.getChildren()) {
                        if (node instanceof Label) {
                            node.setStyle("-fx-background-color: #A0522D ; -fx-padding: 4; -fx-border-color: #FFA07A ; -fx-border-width: 1;");
                        }
                    }
                }
                label.setStyle("-fx-background-color: red; -fx-padding: 4; -fx-border-color: black; -fx-border-width: 1;");
            }



    private void clearCalendarDays(){
        calendar.getChildren().removeIf(node -> node instanceof StackPane);

    }

    @FXML
    void backOneMonth(ActionEvent event) {
        dateFocus = dateFocus.minusMonths(1);
        clearCalendarDays();
        drawCalendar();
    }


    @FXML
    void forwardOneMonth(ActionEvent event) {
        dateFocus = dateFocus.plusMonths(1);
        clearCalendarDays();
        drawCalendar();
    }
    @FXML
    private void updateWeekInfoLabel(){
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        String formattedStart = startOfWeek.format(formatter);
        String formattedEnd = endOfWeek.format(formatter);
        String currentWeek = updateCurrentWeekType();

    }



    private final Map<String, Map<DayOfWeek, List<String>>> scheduleByWeek = new HashMap<>();

    private void initializeSchedule(){
        scheduleByWeek.put("A", new HashMap<>());
        scheduleByWeek.put("B", new HashMap<>());

       // Schedule for week A
        scheduleByWeek.get("A").put(DayOfWeek.MONDAY, List.of("LEC,Numerical Methods,08:45-10:15,Room A61 building A" ,
            "LEC,,Discrete Mathematics 1, 10:30-12:00, Room A61, building A",
            "LEC,,Algorithms and Data Structures, 12:15-13:45, Room A61, building A",
            "LEC,,Computer Systems Architecture, 14:00-15:30, Room A61, building A"));
        scheduleByWeek.get("A").put(DayOfWeek.TUESDAY, List.of("TUT,group 1,Algorithms and Data Structures,10:30-12:00,Room A214 building A",
            "TUT,group 1,Electronics for Computer Science,12:15-13:45,Room A214 building A",
            "LEC,,Electronics for Computer Science, 14:00-15:30, Room A61, building A",
            "LEC,,Computer Logic and Arithmetic Elements,15:45-17:15, Room A61, building A",
            "LEC,,Programming in Java,17:30-19:00, Room A61, building A"));
        scheduleByWeek.get("A").put(DayOfWeek.WEDNESDAY, List.of("LAB,group 1,Computer Logic and Arithmetic Elements,12:15-13:45,Room F604, building F",
            "LAB,group 1,Computer Systems Architecture,14:00-15:30,Room A75, building A",
            "TUT,group 1,Computer Logic and Arithmetic Elements,15:45-17:15,Room A312, building A"));
        scheduleByWeek.get("A").put(DayOfWeek.THURSDAY, List.of("LAB,group 1,Discrete Mathematics 1,08:45-10:15,Room B210, building B","TUT,group 1,Physical Education,19:30-21:00, Swimming Pool"));
        scheduleByWeek.get("A").put(DayOfWeek.FRIDAY, List.of("LAB,group 1,Numerical Methods,Room,8:45-10:15, B205,building B"));
        scheduleByWeek.get("A").put(DayOfWeek.SATURDAY, new ArrayList<>());
        scheduleByWeek.get("A").put(DayOfWeek.SUNDAY, new ArrayList<>());

    // Schedule for week B
        scheduleByWeek.get("B").put(DayOfWeek.MONDAY, List.of("LEC,Numerical Methods,08:45-10:15, Room A61 building A" ,
            "LEC,,Discrete Mathematics 1, 10:30-12:00, Room A61, building A",
            "LEC,,Algorithms and Data Structures, 12:15-13:45, Room A61, building A",
            "LEC,,Computer Systems Architecture, 14:00-15:30, Room A61, building A"));
        scheduleByWeek.get("B").put(DayOfWeek.TUESDAY, List.of("TUT,group 1, Numerical Methods,12:15-13:45,Room A113 building A",
            "LEC,,Electronics for Computer Science,14:00-15:30, Room A61, building A",
            "LEC,,Computer Logic and Arithmetic Elements,15:45-17:15,Room A61, building A",
            "LEC,,Programming in Java,17:30-19:00,Room A61, building A"));
        scheduleByWeek.get("B").put(DayOfWeek.WEDNESDAY, List.of("LAB,group 1,Algorithms and Data Structures,14:00-15:30, Room F105 building F",
            "LAB,group 1,Discrete Mathematics 1,15:45-17:15,Room A312 building A"));
        scheduleByWeek.get("B").put(DayOfWeek.THURSDAY, List.of("LAB,group 1,Computer Systems Architecture,8:45-10:15, Room F206 building F",
            "LAB, group 1, Electronics for Computer Science,11:30-13:45, Room A304 building A",
            "LAB, group 1,Programming in Java,14:00-15:30, Room B100A building A"));
        scheduleByWeek.get("B").put(DayOfWeek.FRIDAY, List.of("PROJ,group 1,Numerical Methods,8:45-10:15,Room B210 building B"));
        scheduleByWeek.get("B").put(DayOfWeek.SATURDAY, new ArrayList<>());
        scheduleByWeek.get("B").put(DayOfWeek.SUNDAY, new ArrayList<>());
    }
    private String updateCurrentWeekType(){
        LocalDate today = LocalDate.now();
        int weekNumber = today.get(WeekFields.of(Locale.getDefault()).weekOfYear());
        return (weekNumber % 2 == 0) ? "A" : "B";
    }

    private void drawCalendar() {
                dayBoxes.clear();
                calendar.getChildren().clear(); // tylko dni, nagłówki są poza tym

                year.setText(String.valueOf(dateFocus.getYear()));
                month.setText(dateFocus.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("us")));

                double calendarWidth = calendar.getPrefWidth();
                double calendarHeight = calendar.getPrefHeight();
                double strokeWidth = 1;
                double spacingH = calendar.getHgap();
                double spacingV = calendar.getVgap();
                Map<Integer, List<CalendarActivity>> calendarActivityMap = getCalendarActivitiesMonth(dateFocus);

                LocalDate firstOfMonth = LocalDate.from(dateFocus.withDayOfMonth(1));
                DayOfWeek firstDayOfWeek = firstOfMonth.getDayOfWeek();
                int shift = (firstDayOfWeek.getValue()+6)% 7; // pon = 1 -> 0
                LocalDate startDate = firstOfMonth.minusDays(shift);

                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 7; j++) {
                        LocalDate currentDate = startDate.plusDays(i * 7 + j);

                        StackPane stackPane = new StackPane();
                        Rectangle rectangle = new Rectangle();
                        rectangle.setFill(Color.TRANSPARENT);
                        rectangle.setStroke(Color.BLACK);
                        rectangle.setStrokeWidth(strokeWidth);

                        double rectangleWidth = ((calendarWidth / 7) - strokeWidth - spacingH);
                        double rectangleHeight = ((calendarHeight / 3.5) - strokeWidth - spacingV);
                        rectangle.setWidth(rectangleWidth);
                        rectangle.setHeight(rectangleHeight);

                        stackPane.getChildren().add(rectangle);

                        Text dateText = new Text(String.valueOf(currentDate.getDayOfMonth()));
                        StackPane.setAlignment(dateText, Pos.TOP_CENTER);
                        dateText.setTranslateX(5);
                        dateText.setTranslateY(5);

                        VBox dayVBox = new VBox(dateText);
                        dayBoxes.add(dayVBox);
                        dayVBox.setAlignment(Pos.TOP_CENTER);
                        dayVBox.setPadding(new Insets(5));

                        if (currentDate.equals(LocalDate.now())) {
                            rectangle.setStroke(Color.RED);
                            rectangle.setStrokeWidth(2);
                        }
                        if (highlightedDate != null && currentDate.equals(highlightedDate)) {
                            rectangle.setStroke(Color.BROWN);
                            rectangle.setStrokeWidth(3);
                        }


                        if (!currentDate.getMonth().equals(dateFocus.getMonth())) {
                            stackPane.setOpacity(0.4);
                        }

                        int weekNumber = currentDate.get(WeekFields.of(Locale.getDefault()).weekOfYear());
                        String weekType = (weekNumber % 2 == 0) ? "A" : "B";
                        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
                        List<String> daySchedule = new ArrayList<>(scheduleByWeek.getOrDefault(weekType, new HashMap<>()).getOrDefault(dayOfWeek, new ArrayList<>()));

                        stackPane.setOnMouseClicked(event -> {
                            StringBuilder details = new StringBuilder("Zajęcia w dniu " + currentDate.getDayOfMonth() + " " + month.getText() + ":\n\n");
                            for (String subject : daySchedule) {
                                details.append(subject).append("\n");
                            }
                            showDetailsWindow(currentDate,daySchedule);
                        });

                        VBox scheduleBox = new VBox();
                        scheduleBox.setPrefWidth(rectangleWidth * 0.9);
                        scheduleBox.setPrefHeight(rectangleHeight * 0.65);
                        scheduleBox.setMaxWidth(rectangleWidth * 0.95);
                        scheduleBox.setMaxHeight(rectangleHeight * 0.95);
                        scheduleBox.setSpacing(3);
                        scheduleBox.setStyle("-fx-background-color: #FFA07A ; -fx-padding:5; -fx-border-radius:5; -fx-background-radius:5;");

                        if (dayOfWeek == DayOfWeek.MONDAY) {
                            Label weekLabel = new Label("Week " + weekType);
                            weekLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: darkgreen;");
                            VBox.setMargin(weekLabel, new Insets(0, 0, 3, 0));
                            scheduleBox.getChildren().add(weekLabel);
                        }

                        int maxlines = 1; // Ograniczamy liczbę wyświetlanych linii

                        for (int k = 0; k < Math.min(daySchedule.size(), maxlines); k++) {
                            String subject = daySchedule.get(k);
                            String displayedText = "";

                            // Logika skracania i formatowania nazwy zajęć
                            String[] parts = subject.split(" "); // Dzielimy nazwę po spacjach
                            if (parts.length > 1) {
                                displayedText = parts[0]; // Pierwsza część (np. "LAB")
                                if (parts[1].length() > 3) {
                                    displayedText += " " + parts[1].substring(0, 3) + "..."; // Skrócona druga część (np. "Met...")
                                } else {
                                    displayedText += " " + parts[1]; // Jeśli krótka, to cała
                                }
                                if (parts.length > 2) {
                                    displayedText += " " + parts[2]; // Dodajemy ewentualnie grupę (np. "gr.1")
                                }
                            } else {
                                displayedText = subject; // Jeśli nazwa jest krótka, to cała
                            }

                            Label label = new Label(displayedText);
                            label.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
                            label.setWrapText(false); // Ważne: Nie zawijamy tekstu!
                            label.setMaxWidth(scheduleBox.getPrefWidth() - 5); // Trochę mniejsza szerokość
                            label.setTooltip(new Tooltip(subject)); // Pełna nazwa w Tooltipie
                            scheduleBox.getChildren().add(label);
                        }

                        if (daySchedule.size() > maxlines) {
                            Button moreButton = new Button("+" + (daySchedule.size() - maxlines) + " more");
                            moreButton.setOnMouseEntered(e -> moreButton.setScaleX(1.1));
                            moreButton.setOnMouseExited(e -> moreButton.setScaleX(1.0));
                            moreButton.setStyle("-fx-font-size: 10px; -fx-background-color: transparent; -fx-text-fill: dark;");
                            moreButton.setOnAction(e -> showDetailsWindow(currentDate,daySchedule));
                            scheduleBox.getChildren().add(moreButton);
                        }

                        StackPane.setAlignment(scheduleBox, Pos.CENTER);
                        scheduleBox.setTranslateY(12);
                        dayVBox.getChildren().add(scheduleBox);
                        stackPane.getChildren().add(dayVBox);

                        if (currentDate.getMonth().equals(dateFocus.getMonth())) {
                            List<CalendarActivity> calendarActivities = calendarActivityMap.get(currentDate.getDayOfMonth());
                            if (calendarActivities != null) {
                                createCalendarActivity(calendarActivities, rectangleHeight, rectangleWidth, stackPane);
                            }
                        }

                        calendar.getChildren().add(stackPane);
                    }
                }



    }
            private Map<LocalDate, List<String>> userPlans = new HashMap<>();


    private void showDetailsWindow(LocalDate selectedDate, List<String> defaultPlans) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.setTitle("Sczegóły dnia - "+selectedDate);
        VBox vbox = new VBox(10);
        vbox.getStyleClass().add("details-vbox");

        Label title = new Label("University courses:");
        title.getStyleClass().add("section-title");
        ListView<String> defaultListView = new ListView<>(FXCollections.observableArrayList(defaultPlans));
        defaultListView.getStyleClass().add("default-list-view");
        Label userTitle = new Label("Your plans:");
        userTitle.getStyleClass().add("section-title");
        List<String> userDayPlans = userPlans.getOrDefault(selectedDate, new ArrayList<>());
        ListView<String> userListView = new ListView<>(FXCollections.observableArrayList(userDayPlans));
        userListView.getStyleClass().add("user-list-view");
        TextField inputField = new TextField();
        inputField.setPromptText("Add plan...");
        inputField.getStyleClass().add("input-field");

        Button addButton = new Button("Add");
        addButton.getStyleClass().add("add-button");
        addButton.setOnAction(e -> {
            String newPlan = inputField.getText().trim();
            if (!newPlan.isEmpty()) {
                        userDayPlans.add(newPlan);
                        userPlans.put(selectedDate, new ArrayList<>(userDayPlans));
                        userListView.getItems().add(newPlan);
                        inputField.clear();
                        saveUserPlansToFile(userPlans);
                }
            });
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete");

        deleteItem.getStyleClass().add("delete-menu-item");
        contextMenu.getItems().add(deleteItem);

        userListView.setContextMenu(contextMenu);

        deleteItem.setOnAction(e -> {
            String selected = userListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                userDayPlans.remove(selected);
                userListView.getItems().remove(selected);
                if (userDayPlans.isEmpty()) {
                    userPlans.remove(selectedDate);
                } else {
                    userPlans.put(selectedDate, new ArrayList<>(userDayPlans));
                }
            }
            saveUserPlansToFile(userPlans);
        });

        Button closeButton = new Button("Close");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> popupStage.close());

        vbox.getChildren().addAll(title, defaultListView, userTitle, userListView, inputField, addButton, closeButton);

        Scene scene = new Scene(vbox, 400, 450);
        scene.getStylesheets().add(getClass().getResource("details_window.css").toExternalForm());
        popupStage.setScene(scene);
        popupStage.show();
    }



    private void createCalendarActivity(List<CalendarActivity> calendarActivities, double rectangleHeight, double rectangleWidth, StackPane stackPane) {
        VBox calendarActivityBox = new VBox();
        for (int k = 0; k < calendarActivities.size(); k++) {
            if(k >= 2) {
                Text moreActivities = new Text("...");
                calendarActivityBox.getChildren().add(moreActivities);
                moreActivities.setOnMouseClicked(mouseEvent -> {
                    System.out.println(calendarActivities);
                });
                break;
            }
            Text text = new Text(calendarActivities.get(k).getClientName() + ", " + calendarActivities.get(k).getDate().toLocalTime());
            calendarActivityBox.getChildren().add(text);
            text.setOnMouseClicked(mouseEvent -> {
                System.out.println(text.getText());
            });
        }
        calendarActivityBox.setTranslateY((rectangleHeight / 2) * 0.20);
        calendarActivityBox.setMaxWidth(rectangleWidth * 0.8);
        calendarActivityBox.setMaxHeight(rectangleHeight * 0.65);
        calendarActivityBox.setStyle("-fx-background-color:#ffa07a");
        stackPane.getChildren().add(calendarActivityBox);
    }

    private Map<Integer, List<CalendarActivity>> createCalendarMap(List<CalendarActivity> calendarActivities) {
        Map<Integer, List<CalendarActivity>> calendarActivityMap = new HashMap<>();

        for (CalendarActivity activity: calendarActivities) {
            int activityDate = activity.getDate().getDayOfMonth();
            if(!calendarActivityMap.containsKey(activityDate)){
                calendarActivityMap.put(activityDate, List.of(activity));
            } else {
                List<CalendarActivity> OldListByDate = calendarActivityMap.get(activityDate);

                List<CalendarActivity> newList = new ArrayList<>(OldListByDate);
                newList.add(activity);
                calendarActivityMap.put(activityDate, newList);
            }
        }
        return  calendarActivityMap;
    }

    private Map<Integer, List<CalendarActivity>> getCalendarActivitiesMonth(ZonedDateTime dateFocus) {
        List<CalendarActivity> calendarActivities = new ArrayList<>();
        int year = dateFocus.getYear();
        int month = dateFocus.getMonth().getValue();

        return createCalendarMap(calendarActivities);
    }


}

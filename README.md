# Student Planner Application 📅

A desktop application designed for students to efficiently manage their schedules, classes, and exams. Built with Java and JavaFX, this project demonstrates practical skills in desktop application development, UI design, backend logic, and data handling.

![Calendar View](assets/screen.jpg) 
## 🚀 Features

* **Interactive Calendar:** An intuitive grid-based calendar interface for navigating through months and years.
* **Schedule Management:** Easily distinguish between different types of classes (Lectures, Labs, Projects, Tutorials) using visual cues and color coding.
* **Exam Tracking:** A dedicated section to manage and view upcoming exams.
* **Search Functionality:** Quickly find specific subjects or classes within your schedule using the built-in search bar.
* **Persistent Storage:** User plans and exam schedules are persistently saved and loaded from local text files (`user_plans.txt`, `egzaminy.txt`).
* **Custom Styling:** Clean and modern UI styled with custom CSS (`details_window.css`).

## 🛠️ Technologies Used

* **Language:** Java (JDK 17+)
* **Framework:** JavaFX (UI Development)
* **Build Tool:** Maven
* **Design:** FXML / Scene Builder 
* **IDE:** IntelliJ IDEA / VS Code 

## 📁 Project Structure

* `src/main/java/com/example/demo/` - Core Java source code.
  * `Main.java` - The entry point of the application.
  * `CalendarController.java` - Handles the logic and user interactions for the main calendar view.
  * `ExamsController.java` - Manages the logic for the exams view.
  * `Exam.java` - Data model representing exam objects.
* `src/main/resources/com/example/demo/` - UI definitions and styling.
  * `hello-view.fxml` & `ExamsView.fxml` - Define the layout of the application's screens.
  * `details_window.css` - Custom styling for the application's components.

## ⚙️ How to Run

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YourUsername/YourRepositoryName.git](https://github.com/YourUsername/YourRepositoryName.git)
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd YourRepositoryName
    ```
3.  **Run with Maven:**
    Ensure you have Maven installed, then execute:
    ```bash
    mvn clean javafx:run
    ```
    *Alternatively, you can open the project in your preferred IDE and run the `Main.java` class directly.*

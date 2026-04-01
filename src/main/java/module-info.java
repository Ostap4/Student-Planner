module com.example.demo {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.naming;

    opens com.example.demo to javafx.fxml;
    exports com.example.demo;
}

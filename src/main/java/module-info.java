module org.example {
    requires javafx.controls;
    requires javafx.fxml;

    opens org.example to javafx.fxml;
    exports org.example;
    exports com.mycompany;
    opens com.mycompany to javafx.fxml;
    opens com.mycompany.Controller to javafx.fxml;

}

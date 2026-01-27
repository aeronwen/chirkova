module org.kp.chirkova {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.kp.chirkova to javafx.fxml;
    exports org.kp.chirkova;
}
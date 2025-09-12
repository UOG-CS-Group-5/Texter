module com.csgroupfive.texter {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.csgroupfive.texter to javafx.fxml;
    exports com.csgroupfive.texter;
}

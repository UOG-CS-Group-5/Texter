module com.csgroupfive.texter {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.json;

    opens com.csgroupfive.texter to javafx.fxml;
    exports com.csgroupfive.texter;
}

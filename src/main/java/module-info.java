module com.csgroupfive.texter {
    requires transitive javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive java.mail;
    requires transitive org.json;
    requires io.github.cdimascio.dotenv.java;
    requires java.net.http;
    opens com.csgroupfive.texter.controllers to javafx.fxml;
    exports com.csgroupfive.texter;
}

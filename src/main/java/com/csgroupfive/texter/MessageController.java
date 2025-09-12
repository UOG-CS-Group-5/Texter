package com.csgroupfive.texter;

import java.io.IOException;
import javafx.fxml.FXML;

public class MessageController {

    @FXML
    private void switchToRecipients() throws IOException {
        App.setRoot("recipients");
    }
}

package com.csgroupfive.texter;

import java.io.IOException;
import javafx.fxml.FXML;

public class RecipientsController {

    @FXML
    private void switchToMessage() throws IOException {
        App.setRoot("message");
    }
}
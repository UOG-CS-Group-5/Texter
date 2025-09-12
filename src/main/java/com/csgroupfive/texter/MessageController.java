package com.csgroupfive.texter;

import java.io.IOException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class MessageController {
    // message textarea
    @FXML TextArea messageArea;

    @FXML
    private void switchToRecipients() throws IOException {
        App.setRoot("recipients");
    }

    @FXML
    public void initialize() {
        // set message textarea to the contents of the data saved in the data file
        messageArea.setText(StoreSingleton.getInstance().getMessage());

        // add a change listener that saves the message on each change
        // TODO: debounce this
        messageArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                StoreSingleton.getInstance().setMessage(messageArea.getText());
            }
        });
    }
}

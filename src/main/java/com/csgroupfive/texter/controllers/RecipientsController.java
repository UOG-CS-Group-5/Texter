package com.csgroupfive.texter.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.csgroupfive.texter.App;
import com.csgroupfive.texter.StoreSingleton;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class RecipientsController {
    // recipients textarea
    @FXML TextArea recipientsArea;

    @FXML
    private void switchToMessage() throws IOException {
        App.setRoot("message");
    }

    @FXML
    public void initialize() {
        
        // set text area to recipients list joined on newline
        List<String> recipients = StoreSingleton.getInstance().getRecipients();
        recipientsArea.setText(String.join("\n", recipients));

        // add a change listener to text area to save recipients on each edit
        // TODO: debounce this
        recipientsArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                saveRecipients();
            }
        });
    }

    public void saveRecipients() {
        // split into list of strings
        String[] recipientsArr = recipientsArea.getText().split("\n|,");
        List<String> recipientsList = new ArrayList<String>(Arrays.asList(recipientsArr));


        recipientsList = recipientsList.stream()
                                       .map(s -> {
                                            if (s.indexOf('@') == -1) {
                                                // remove non-numeric
                                                return s.replaceAll("\\D", "");
                                            } else {
                                                // if it's an email address, just strip whitespaces
                                                return s.strip();
                                            }
                                       })
                                       .filter(s -> {
                                            if (s.indexOf('@') == -1) {
                                                // filter out non-10 digit numbers
                                                return s != null && s.length() == 10;
                                            } else {
                                                // filter out things that don't look like an email. rudimentary check
                                                return s.indexOf('.') > s.indexOf('@') && s.length() >= 6;
                                            }
                                       })
                                       .collect(Collectors.toList());

        // save recipients
        StoreSingleton.getInstance().setRecipients(recipientsList);
    }
}
package com.csgroupfive.texter.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.csgroupfive.texter.App;
import com.csgroupfive.texter.StoreSingleton;
import com.csgroupfive.texter.senders.EmailSender;
import com.csgroupfive.texter.senders.EmailToSmsSender;
import com.csgroupfive.texter.senders.GreenApi;
import com.csgroupfive.texter.senders.util.ApiResponseStatus;
import com.csgroupfive.texter.senders.util.Emailer;
import com.csgroupfive.texter.senders.util.Messagable;
import com.csgroupfive.texter.senders.util.Sender;
import com.csgroupfive.texter.senders.util.SenderType;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class MessageController {
    // message textarea
    @FXML TextArea recipientsArea;
    @FXML TextArea messageArea;
    @FXML TextArea subjectArea;
    @FXML Button sendButton, savebutton, clearbutton;
    @FXML ScrollPane sp_main;
    @FXML VBox vbox_msg;
    @FXML Button newMsgButton;

    private GreenApi greenApi = new GreenApi();
    private EmailToSmsSender gtaEmailToSms = new EmailToSmsSender("", "sms.gta.net");
    private EmailSender regularEmailer = new EmailSender();
    // multiple potential endpoints
    private Messagable[] messagables = {greenApi, gtaEmailToSms, regularEmailer};
    private boolean animationPlaying = false;
    private boolean suppressTextListener = false;
    private boolean suppressRecipientsListener = false;
    private int selectedIndex = -1;
    private Text selectedBubbleText;

    @FXML
    private void switchToRecipients() throws IOException {
        App.setRoot("recipients");
    }

    @FXML
    public void initialize() {
        selectedIndex = -1;
        selectedBubbleText = null;
        newMsgButton.setVisible(false);
        if (subjectArea != null) subjectArea.clear();
        if (messageArea != null) messageArea.clear();
        StoreSingleton.getInstance().setMessage("");

        // One listener that watches both subject and body
        ChangeListener<String> toggleNewMsg = (obs, ov, nv) -> {
            if (suppressTextListener) return;

            String subj = subjectArea != null && subjectArea.getText() != null ? subjectArea.getText().strip() : "";
            String body = messageArea != null && messageArea.getText() != null ? messageArea.getText().strip() : "";
            boolean hasAny = !subj.isEmpty() || !body.isEmpty();
            newMsgButton.setVisible(hasAny);

            // live update or delete while editing a saved item
            if (selectedIndex >= 0) {
                if (subj.isEmpty() && body.isEmpty()) {
                    StoreSingleton.getInstance().removeSavedMessage(selectedIndex);
                    refreshSavedMessagesUI(false);
                    selectedIndex = -1;
                    selectedBubbleText = null;

                    suppressTextListener = true;
                    try {
                        subjectArea.clear();
                        messageArea.clear();
                        newMsgButton.setVisible(false);
                    } finally {
                        Platform.runLater(() -> suppressTextListener = false);
                    }
                } else {
                    String combined = combineSubjectBody(subj, body);
                    StoreSingleton.getInstance().updateSavedMessage(selectedIndex, combined);
                }
            }
        };

        if (subjectArea != null) subjectArea.textProperty().addListener(toggleNewMsg);
        if (messageArea != null) messageArea.textProperty().addListener(toggleNewMsg);

        if (recipientsArea != null) {
            // Load recipients into the textarea
            List<String> recipients = StoreSingleton.getInstance().getRecipients();
            recipientsArea.setText(String.join("\n", recipients));

            // Save on each edit
            recipientsArea.textProperty().addListener(new ChangeListener<String>() {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                    if (suppressRecipientsListener) return;
                    saveRecipients();  // call the method below
                }
            });
        }

        //Save
        if (savebutton != null) {
            savebutton.setOnAction(e -> saveCurrentMessage());
        }
        //Clear
        if (clearbutton != null) {
            clearbutton.setOnAction(e -> {
                if (subjectArea != null) subjectArea.clear();  // clear subject
                if (messageArea != null) messageArea.clear(); // clear body
                newMsgButton.setVisible(false); // hide when cleared
                selectedIndex = -1;
                selectedBubbleText = null;
            });
        }
        if (sp_main != null) {
            sp_main.setFitToWidth(true);
            vbox_msg.setFillWidth(true);


            Platform.runLater(() ->
                    vbox_msg.setPrefWidth(sp_main.getViewportBounds().getWidth())
            );

            // keep it in sync later
            sp_main.viewportBoundsProperty().addListener((obs, ov, nv) -> {
                vbox_msg.setPrefWidth(nv.getWidth());
            });

            sp_main.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            sp_main.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
            sp_main.setFitToHeight(false);
            sp_main.setPannable(true);
        }


        // saved messages show at startup
        refreshSavedMessagesUI(false);

        // when send button is pressed
        sendButton.setOnAction(e -> {
            // if text area is empty
            if (messageArea.getText().strip().length() == 0) {
                // show some user feedback telling user to fill text area
                showInfo("Nothing to send");
            } else {
                // grab message and list of recipients
                String message = messageArea.getText().strip();
                List<String> recipients = StoreSingleton.getInstance().getRecipients();
                // for each recipient
                for (String r : recipients) {
                    ApiResponseStatus status = null;
                    // for each messageable endpoint
                    for (Messagable m : messagables) {

                        boolean isEmail = r.indexOf('@') != -1;
                        SenderType senderType = ((Sender) m).getType();
                        // differentiating types here so we can later add a subject field if wanted
                        if (isEmail) {
                            if (senderType == SenderType.EMAIL) {
                                // TODO: add subject and/or from field once @Frances adds subject box
                                status = ((Emailer) m).send_email(message, r);
                            }
                        } else {
                            if (senderType == SenderType.SMS) {
                                status = m.send_message(message, r);
                            }
                        }
                        // if successful, move on to the next recipient
                        if (status == ApiResponseStatus.SUCCESS) {
                            break;
                        }
                        // TODO: possibly display unknowns and failures to user
                        // @Francis, I'll let you decide
                        System.err.println(((Sender) m).getName() + " " + status);
                    }
                    // TODO: display error to user. 
                    // @Francis, here's a UI task :)
                    System.err.println(status);
                }
            }
        });
    }
    private void saveRecipients() {
        if (recipientsArea == null) return;

        String raw = recipientsArea.getText();
        if (raw == null) raw = "";

        // Split by newline or comma
        String[] recipientsArr = raw.split("\n|,");
        List<String> recipientsList = new ArrayList<>(Arrays.asList(recipientsArr));

        // Normalize: keep digits only, require 10 digits
        recipientsList = recipientsList.stream()
                .map(s -> s == null ? "" : s.replaceAll("\\D", ""))
                .filter(s -> s.length() == 10)
                .collect(Collectors.toList());

        StoreSingleton.getInstance().setRecipients(recipientsList);
    }

    private static String combineSubjectBody(String subject, String body) {
        subject = subject == null ? "" : subject.strip();
        body    = body    == null ? "" : body.strip();

        if (subject.isEmpty()) return body;          // store body only
        if (body.isEmpty())    return subject;       // store subject only
        return subject + "\n" + body;                // store subject + body
    }

    private void saveCurrentMessage() {
        String subj = subjectArea.getText() == null ? "" : subjectArea.getText().trim();
        String body = messageArea.getText() == null ? "" : messageArea.getText().trim();

        if (subj.isEmpty() && body.isEmpty()) {
            showInfo("Nothing to save");
            return;
        }

        String combined = combineSubjectBody(subj, body);

        if (selectedIndex >= 0) {
            StoreSingleton.getInstance().updateAndMoveSavedMessageToFront(selectedIndex, combined);
            selectedIndex = -1;
            selectedBubbleText = null;
        } else {
            StoreSingleton.getInstance().prependSavedMessage(combined);
        }

        refreshSavedMessagesUI(true);

        suppressTextListener = true;
        try {
            subjectArea.clear();
            messageArea.clear();
            newMsgButton.setVisible(false);
        } finally {
            suppressTextListener = false;
        }
    }


    // Refresh saved messages
    private void refreshSavedMessagesUI(boolean scrollToTop) {
        if (vbox_msg == null) return;
        vbox_msg.getChildren().clear();

        java.util.List<String> saved = StoreSingleton.getInstance().getSavedMessages();
        for (int i = 0; i < saved.size(); i++) {
            addBubbleToVBox(saved.get(i), i); // pass real index
        }

        if (scrollToTop) {
            Platform.runLater(() -> sp_main.setVvalue(0.0));
        }
    }

    private static String[] parseSubjectBody(String stored) {
        if (stored == null) return new String[] {"", ""};
        int idx = stored.indexOf('\n');
        if (idx >= 0) {
            String subject = stored.substring(0, idx);
            String body = stored.substring(idx + 1);
            return new String[] {subject, body};
        } else {
            // legacy one liners are body only
            return new String[] {"", stored};
        }
    }

    // create one bubble and add to the vbox
    private void addBubbleToVBox(String text, int indexInStore) {
        Node bubbleRow = createMessageBubble(text, indexInStore);
        vbox_msg.getChildren().add(bubbleRow);
    }

    private Node createMessageBubble(String stored, int indexInStore) {
        final String[] parsed = parseSubjectBody(stored);
        final String subjectText = parsed[0];
        final String bodyText    = parsed[1];

        Label subjectLabel = new Label(subjectText);
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000; -fx-font-size: 13px;");
        subjectLabel.setWrapText(false);
        subjectLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        subjectLabel.setMaxWidth(Double.MAX_VALUE);

        Label bodyLabel = new Label(bodyText);
        bodyLabel.setStyle("-fx-text-fill: #132a13; -fx-font-size: 12px;");
        bodyLabel.setWrapText(false);
        bodyLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        VBox inner = new VBox(subjectLabel, bodyLabel);
        inner.setSpacing(2);
        inner.setPadding(new Insets(0));
        inner.setMaxWidth(Double.MAX_VALUE);

        VBox container = new VBox(inner);
        container.getStyleClass().add("bubble"); // use CSS class
        container.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(container);
        row.setPadding(new Insets(0));
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);

        row.prefWidthProperty().bind(vbox_msg.widthProperty());
        container.prefWidthProperty().bind(row.widthProperty());
        inner.prefWidthProperty().bind(container.widthProperty());

        row.setOnMouseClicked(ev -> {
            selectedIndex = indexInStore;
            suppressTextListener = true;
            try {
                if (subjectArea != null) subjectArea.setText(subjectText);
                if (messageArea != null) messageArea.setText(bodyText);
                if (newMsgButton != null) {
                    newMsgButton.setVisible(!(subjectText.isBlank() && bodyText.isBlank()));
                }
            } finally {
                Platform.runLater(() -> suppressTextListener = false);
            }

            // Visual feedback without changing layout: quick fade flash
            container.setOpacity(0.85);
            PauseTransition t = new PauseTransition(Duration.millis(150));
            t.setOnFinished(_x -> container.setOpacity(1.0));
            t.play();
        });

        return row;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    @FXML
    private void onNewMessage() {
        suppressTextListener = true;
        try {
            selectedIndex = -1;
            selectedBubbleText = null;

            subjectArea.clear();
            messageArea.clear();
            StoreSingleton.getInstance().setMessage("");
            if (newMsgButton != null) newMsgButton.setVisible(false);
            subjectArea.requestFocus();
        } finally {
            suppressTextListener = false;
        }
    }
}

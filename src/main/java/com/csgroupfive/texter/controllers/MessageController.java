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

import javafx.animation.PauseTransition;
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
    private boolean removingSelected = false;

    @FXML
    public void initialize() {
        // Resetting State
        selectedIndex = -1;
        selectedBubbleText = null;
        newMsgButton.setVisible(false);

        // Clears the subject box and body box if they exist
        if (subjectArea != null) subjectArea.clear();
        if (messageArea != null) messageArea.clear();

        // Resets the stored “current message” in StoreSingleton to empty.
        StoreSingleton.getInstance().setMessage("");

        // Listener for subject and message areas
        ChangeListener<String> toggleNewMsg = (obs, ov, nv) -> {
            // Immediately exits if suppressTextListener is true
            if (suppressTextListener) return;

            // Reads the subject and body
            String subj = subjectArea != null && subjectArea.getText() != null ? subjectArea.getText().strip() : "";
            String body = messageArea != null && messageArea.getText() != null ? messageArea.getText().strip() : "";

            // Show new message button if text area isn't empty
            boolean hasAny = !subj.isEmpty() || !body.isEmpty();
            newMsgButton.setVisible(hasAny);

            // Updating and removing saved messaages
            if (selectedIndex >= 0) {
                // if editing a saved message and the text becomes empty
                if (subj.isEmpty() && body.isEmpty()) {
                    // remove it from saved messages
                    StoreSingleton.getInstance().removeSavedMessage(selectedIndex);
                    // refresh list
                    refreshSavedMessagesUI(false);
                    //reset selection
                    selectedIndex = -1;
                    selectedBubbleText = null;

                    suppressTextListener = true; // disable listener
                    try {
                        // clear text areas
                        subjectArea.clear();
                        messageArea.clear();
                        newMsgButton.setVisible(false);
                    } finally {
                        // re-enable listener
                        Platform.runLater(() -> suppressTextListener = false);
                    }
                } else {
                    // if not empty, update the saved message with new subject + body
                    String combined = combineSubjectBody(subj, body);
                    StoreSingleton.getInstance().updateSavedMessage(selectedIndex, combined);
                }
            }
        };

        // Connects the listener to both subject and message text areas.
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

        // ScrollPane and VBox setup
        if (sp_main != null) {
            // Fit messages to width.
            sp_main.setFitToWidth(true);
            vbox_msg.setFillWidth(true);

            // Dynamically update width when the viewport changes.
            Platform.runLater(() ->
                    vbox_msg.setPrefWidth(sp_main.getViewportBounds().getWidth())
            );
            sp_main.viewportBoundsProperty().addListener((obs, ov, nv) -> {
                vbox_msg.setPrefWidth(nv.getWidth());
            });

            // No horizontal scroll bar, only vertical.
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
        // If the UI text area for recipients is missing, exit.
        if (recipientsArea == null) return;

        // Read the raw text from the recipients box.
        String raw = recipientsArea.getText();
        // If it is null, treat it as an empty string.
        if (raw == null) raw = "";

        // split on newline, comma, or semicolon
        String[] tokens = raw.split("[\\n,;]+");

        List<String> cleaned = Arrays.stream(tokens)
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    if (s.contains("@")) {
                        // keep emails as-is
                        return s;
                    }
                    // normalize phone
                    String digits = s.replaceAll("\\D", "");
                    if (digits.length() == 11 && digits.startsWith("1")) {
                        digits = digits.substring(1); // drop leading country code
                    }
                    return digits;
                })
                .filter(s -> s.contains("@") || s.length() == 10)
                .distinct()
                .collect(Collectors.toList());

        StoreSingleton.getInstance().setRecipients(cleaned);
    }


    private static String combineSubjectBody(String subject, String body) {
        // trim subject and body.
        subject = subject == null ? "" : subject.strip();
        body    = body    == null ? "" : body.strip();

        // If one is empty, return the other alone.
        if (subject.isEmpty()) return body;          // store body only
        if (body.isEmpty())    return subject;       // store subject only
        // If both exist, join with a newline so you can split later.
        return subject + "\n" + body;                // store subject + body
    }

    private void saveCurrentMessage() {
        // Read and trim the current subject and body
        String subj = subjectArea.getText() == null ? "" : subjectArea.getText().trim();
        String body = messageArea.getText() == null ? "" : messageArea.getText().trim();

        // If both are empty, show an info alert and stop.
        if (subj.isEmpty() && body.isEmpty()) {
            showInfo("Nothing to save");
            return;
        }

        String combined = combineSubjectBody(subj, body);

        // If editing an existing saved message, update it and bring it to the front of the list, then clear selection.
        if (selectedIndex >= 0) {
            StoreSingleton.getInstance().updateAndMoveSavedMessageToFront(selectedIndex, combined);
            selectedIndex = -1;
            selectedBubbleText = null;
        } else {
            // If adding a new one, prepend it to the list.
            StoreSingleton.getInstance().prependSavedMessage(combined);
        }

        // Rebuild the message list UI, and request scroll to top.
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
        // If the VBox that holds message bubbles is missing, exit.
        if (vbox_msg == null) return;
        // Otherwise clear all existing bubble nodes.
        vbox_msg.getChildren().clear();

        // Fetch saved messages from the store.
        java.util.List<String> saved = StoreSingleton.getInstance().getSavedMessages();
       // For each saved message, create and add a bubble, passing its index used by the store.
        for (int i = 0; i < saved.size(); i++) {
            addBubbleToVBox(saved.get(i), i); // pass real index
        }

        // Scroll to top
        if (scrollToTop) {
            Platform.runLater(() -> sp_main.setVvalue(0.0));
        }
    }

    private static String[] parseSubjectBody(String stored) {
        // If nothing stored, return empty subject and body
        if (stored == null) return new String[] {"", ""};

        // If a newline exists, split at the first newline. Left is subject, right is body.
        int idx = stored.indexOf('\n');
        if (idx >= 0) {
            String subject = stored.substring(0, idx);
            String body = stored.substring(idx + 1);
            return new String[] {subject, body};
        } else {
            // If no newline, treat the entire string as body and subject as empty.
            return new String[] {"", stored};
        }
    }

    // Create "bubble" and add to VBox
    private void addBubbleToVBox(String text, int indexInStore) {
        Node bubbleRow = createMessageBubble(text, indexInStore);
        vbox_msg.getChildren().add(bubbleRow);
    }

    private Node createMessageBubble(String stored, int indexInStore) {
        // Split the stored text into subject and body.
        final String[] parsed = parseSubjectBody(stored);
        final String subjectText = parsed[0];
        final String bodyText    = parsed[1];

        // Create subject Label
        Label subjectLabel = new Label(subjectText);
        subjectLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #000000; -fx-font-size: 13px;");
        subjectLabel.setWrapText(false);
        subjectLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        subjectLabel.setMaxWidth(Double.MAX_VALUE);

        // Create Body Label
        Label bodyLabel = new Label(bodyText);
        bodyLabel.setStyle("-fx-text-fill: #132a13; -fx-font-size: 12px;");
        bodyLabel.setWrapText(false);
        bodyLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        bodyLabel.setMaxWidth(Double.MAX_VALUE);

        // Stack subject and body vertically inside an inner VBox.
        VBox inner = new VBox(subjectLabel, bodyLabel);
        inner.setSpacing(2);
        inner.setPadding(new Insets(0));
        inner.setMaxWidth(Double.MAX_VALUE);

        // Wrap the inner VBox in a container VBox.
        VBox container = new VBox(inner);
        container.getStyleClass().add("bubble"); // use CSS class
        container.setMaxWidth(Double.MAX_VALUE);

        // Put the VBox container into an HBox.
        HBox row = new HBox(container);
        row.setPadding(new Insets(0));
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(container, javafx.scene.layout.Priority.ALWAYS);

        // Bind widths so the bubble tracks the parent VBox width
        row.prefWidthProperty().bind(vbox_msg.widthProperty());
        container.prefWidthProperty().bind(row.widthProperty());
        inner.prefWidthProperty().bind(container.widthProperty());

        // When saved message is clicked
        row.setOnMouseClicked(ev -> {
            // Remember which saved item is selected
            selectedIndex = indexInStore;
            suppressTextListener = true;
            try {
                // Set the subject and body fields in the editor
                if (subjectArea != null) subjectArea.setText(subjectText);
                if (messageArea != null) messageArea.setText(bodyText);
                if (newMsgButton != null) {
                    // Show the New Message button if there is any text.
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

    // Create an information alert with the given message
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
            subjectArea.requestFocus(); // Put cursor on subject field
        } finally {
            suppressTextListener = false;
        }
    }
}

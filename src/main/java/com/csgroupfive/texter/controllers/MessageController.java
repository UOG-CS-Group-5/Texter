package com.csgroupfive.texter.controllers;

import java.io.IOException;
import java.util.List;

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
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.control.Alert;
import javafx.util.Duration;

public class MessageController {
    // message textarea
    @FXML TextArea messageArea;
    @FXML Button sendButton, savebutton;
    @FXML ScrollPane sp_main;
    @FXML VBox vbox_msg;

    private GreenApi greenApi = new GreenApi();
    private EmailToSmsSender gtaEmailToSms = new EmailToSmsSender("", "sms.gta.net");
    private EmailSender regularEmailer = new EmailSender();
    // multiple potential endpoints
    private Messagable[] messagables = {greenApi, gtaEmailToSms, regularEmailer};
    private boolean animationPlaying = false;
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
        // set message textarea to the contents of the data saved in the data file
        messageArea.setText(StoreSingleton.getInstance().getMessage());

        // add a change listener that saves the message on each change
        // TODO: debounce this
        messageArea.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                StoreSingleton.getInstance().setMessage(messageArea.getText());

                // live update the selected saved message
                if (selectedIndex >= 0) {
                    StoreSingleton.getInstance().updateSavedMessage(selectedIndex, newValue);
                    if (selectedBubbleText != null) {
                        selectedBubbleText.setText(newValue);
                    }
                }
            }
        });

        //set up Save and Clear
        if (savebutton != null) {
            savebutton.setOnAction(e -> saveCurrentMessage());
        }
        if (sp_main != null) {
            sp_main.setFitToWidth(true);
        }

        // saved messages show at startup
        refreshSavedMessagesUI();

        // when send button is pressed
        sendButton.setOnAction(e -> {
            // if text area is empty
            if (messageArea.getText().strip().length() == 0) {
                // show some user feedback telling user to fill text area
                userFeedbackEmpty();
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


    // save current textarea content and render a bubble
    private void saveCurrentMessage() {
        String text = messageArea.getText() == null ? "" : messageArea.getText().trim();
        if (text.isEmpty()) {
            showInfo("Nothing to save");
            return;
        }

        if (selectedIndex >= 0) {
            StoreSingleton.getInstance().updateAndMoveSavedMessageToFront(selectedIndex, text);
            selectedIndex = -1;
            selectedBubbleText = null;
        } else {
            StoreSingleton.getInstance().prependSavedMessage(text);
        }

        refreshSavedMessagesUI();
        Platform.runLater(() -> sp_main.setVvalue(0.0)); // top
        messageArea.clear();
    }

    // Refresh saved messages
    private void refreshSavedMessagesUI() {
        if (vbox_msg == null) return;
        vbox_msg.getChildren().clear();

        java.util.List<String> saved = StoreSingleton.getInstance().getSavedMessages();
        for (int i = 0; i < saved.size(); i++) {
            addBubbleToVBox(saved.get(i), i); // pass real index
        }

        Platform.runLater(() -> sp_main.setVvalue(0.0)); // show top
    }

    // create one bubble and add to the vbox
    private void addBubbleToVBox(String text, int indexInStore) {
        Node bubbleRow = createMessageBubble(text, indexInStore);
        vbox_msg.getChildren().add(bubbleRow); // append
    }

    // Click to load back to textarea
    private Node createMessageBubble(String text, int indexInStore) {
        Text content = new Text(text);
        content.wrappingWidthProperty().bind(vbox_msg.widthProperty().subtract(32));

        VBox bubble = new VBox(content);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setStyle(
                "-fx-background-color: #e8f0ff;" +
                        " -fx-background-radius: 10;" +
                        " -fx-border-color: #c6d7ff;" +
                        " -fx-border-radius: 10;"
        );
        bubble.maxWidthProperty().bind(vbox_msg.widthProperty().subtract(16));

        HBox row = new HBox(bubble);
        row.setPadding(new Insets(6, 8, 6, 8));

        row.setOnMouseClicked(ev -> {
            // set current selection
            selectedIndex = indexInStore;
            selectedBubbleText = content;

            // load into editor
            messageArea.setText(StoreSingleton.getInstance().getSavedMessages().get(indexInStore));

            // small visual feedback
            bubble.setStyle(
                    "-fx-background-color: #dbe7ff;" +
                            " -fx-background-radius: 10;" +
                            " -fx-border-color: #aac4ff;" +
                            " -fx-border-radius: 10;"
            );
            PauseTransition t = new PauseTransition(Duration.millis(150));
            t.setOnFinished(_x -> bubble.setStyle(
                    "-fx-background-color: #e8f0ff;" +
                            " -fx-background-radius: 10;" +
                            " -fx-border-color: #c6d7ff;" +
                            " -fx-border-radius: 10;"
            ));
            t.play();
        });

        return row;
    }

    private void showInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void userFeedbackEmpty() {
        float bounceTime = 0.125f;
        int bounceAmt = 4;

        // if already animating, don't start a new one
        if (animationPlaying) {return;}
        animationPlaying = true;

        // set button text to "Empty"
        sendButton.setText("Empty");

        // Set it back some time later
        PauseTransition pause = new PauseTransition(Duration.seconds(bounceAmt * bounceTime));
        pause.setOnFinished(e -> {
            sendButton.setText("Send");
            // once done, remove animating flag
            animationPlaying = false;
        });
        pause.play();

        // bounce text area back and forth
        TranslateTransition tt = new TranslateTransition(Duration.seconds(bounceTime), messageArea);
        // by 4 px
        tt.setByX(-4);
        // this many times (full cycles = amt/2)
        tt.setCycleCount(bounceAmt);
        // go back to original position every odd cycle
        tt.setAutoReverse(true);
        // ease in/out (smoothing)
        tt.setInterpolator(Interpolator.EASE_BOTH);
        tt.play();
    }
}

package com.csgroupfive.texter.controllers;

import java.io.IOException;
import java.util.List;

import com.csgroupfive.texter.App;
import com.csgroupfive.texter.StoreSingleton;
import com.csgroupfive.texter.senders.EmailSender;
import com.csgroupfive.texter.senders.GreenApi;
import com.csgroupfive.texter.senders.util.ApiResponseStatus;
import com.csgroupfive.texter.senders.util.Messagable;
import com.csgroupfive.texter.senders.util.Named;

import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.util.Duration;

public class MessageController {
    // message textarea
    @FXML TextArea messageArea;
    @FXML Button sendButton;

    private GreenApi greenApi = new GreenApi();
    private EmailSender gtaEmailToSms = new EmailSender("", "sms.gta.net");
    // multiple potential endpoints
    private Messagable[] messagables = {greenApi, gtaEmailToSms};
    private boolean animationPlaying = false;

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
                        // attempt to send a message
                        status = m.send_message(message, r);
                        // if successful, move on to the next recipient
                        if (status == ApiResponseStatus.SUCCESS) {
                            break;
                        }
                        // TODO: possibly display unknowns and failures to user
                        // @Francis, I'll let you decide
                        System.err.println(((Named) m).getName() + " " + status);
                    }
                    // TODO: display error to user. 
                    // @Francis, here's a UI task :)
                    System.err.println(status);
                }
            }
        });
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

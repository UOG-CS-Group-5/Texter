package com.csgroupfive.texter.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Pane;

public class LogController {

    @FXML private Button closeLogButton;
    @FXML private TextArea logText;
    
    private MessageController parentController;

    public boolean closed = false;
    
    @FXML
    private void initialize() {
        this.closed = false;
        closeLogButton.setOnAction(this::handleClose);
    }

    public void setParent(MessageController cont) {
        parentController = cont;
    }

    public void log(String txt) {
        logText.setText(logText.getText() + '\n' + txt);
        logText.positionCaret(logText.getText().length());
    }

    private void handleClose(ActionEvent event) {
        System.err.println("closing");
        // Get the button that triggered the event
        Node source = (Node) event.getSource();

        // Get the root node of this component (the HBox)
        Node root = source.getParent();

        // Get the parent container (e.g. VBox) and remove this component
        if (root.getParent() != null && root.getParent() instanceof Pane) {
            Pane parentContainer = (Pane) root.getParent();
            parentContainer.getChildren().remove(root);
        }
        this.parentController.adjustAfterLogClose();
        this.closed = true;
        System.err.println("closed");
    }
}

package com.example.drafts.controllers;

import com.example.drafts.SceneManager;
import com.example.drafts.utils.Animations;
import com.example.drafts.utils.Notification;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.io.IOException;

public class Sidebar {
    @FXML private Button profile;
    @FXML private Button chat;
    @FXML private Button group;
    @FXML private Button settings;
    @FXML private VBox root;

    private static final PseudoClass LOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("locked");

    public void initialize() {
        chat.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, true);
        Animations.fadeIn(root);
    }

    @FXML
    private void groupChatScene() {
        unlockButtons();
        group.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, true);
        AppView.switchView("group_chat.fxml");
    }

    @FXML
    private void logOut() throws IOException {
        Notification.show("Logout successful!", Notification.Type.SUCCESS);
        SceneManager.switchTo("login_signup.fxml", "login_signup.css");
    }

    @FXML
    private void messageScene() {
        unlockButtons();
        chat.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, true);
        AppView.switchView("message_view.fxml");
    }

    @FXML
    private void settingsScene() {
        unlockButtons();
        settings.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, true);
        AppView.switchView("settings.fxml");
    }

    private void unlockButtons() {
        chat.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, false);
        group.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, false);
        settings.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, false);
        profile.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, false);
    }

    @FXML
    private void profileScene() {
        unlockButtons();
        profile.pseudoClassStateChanged(LOCKED_PSEUDO_CLASS, true);
        AppView.switchView("profile_view.fxml");
    }
}

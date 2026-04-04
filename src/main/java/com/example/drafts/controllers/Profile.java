package com.example.drafts.controllers;

import com.example.drafts.*;
import com.example.drafts.utils.Animations;
import com.example.drafts.utils.Notification;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.*;

public class Profile {
    @FXML private VBox root;
    @FXML private ImageView profilePicView;
    @FXML private TextField nameField;
    @FXML private PasswordField currentPassword;
    @FXML private PasswordField newPassword;
    @FXML private PasswordField confirmPassword;

    @FXML
    public void initialize() {
        int id = Session.getCurrentUserId();
        nameField.setText(Database.getNameById(id));

        byte[] pic = Database.getProfilePicture(id);
        if (pic != null) {
            profilePicView.setImage(new Image(new ByteArrayInputStream(pic)));
        } else {
            profilePicView.setImage(new Image(
                    getClass().getResourceAsStream("/com/example/drafts/icons/user.png")
            ));
        }
        Animations.fadeIn(root);
    }

    @FXML
    private void choosePicture() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Picture");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(profilePicView.getScene().getWindow());
        if (file == null) return;

        try {
            byte[] bytes = ImageHandler.processProfileImage(file);
            boolean ok = Database.updateProfilePicture(Session.getCurrentUserId(), bytes);
            if (ok) {
                profilePicView.setImage(new Image(new ByteArrayInputStream(bytes)));
                Notification.show("Profile picture updated!", Notification.Type.SUCCESS);
            } else {
                Notification.show("Failed to update picture.", Notification.Type.ERROR);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Notification.show("Could not read image file.", Notification.Type.ERROR);
        }
    }

    @FXML
    private void removePicture() {
        boolean ok = Database.updateProfilePicture(Session.getCurrentUserId(), null);
        if (ok) {
            profilePicView.setImage(new Image(
                    getClass().getResourceAsStream("/com/example/drafts/icons/user.png")
            ));
            Notification.show("Profile picture removed.", Notification.Type.INFO);
        }
    }

    @FXML
    private void saveName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            Notification.show("Name cannot be empty.", Notification.Type.ERROR);
            return;
        }
        boolean ok = Database.updateName(Session.getCurrentUserId(), name);
        if (ok) Notification.show("Name updated!", Notification.Type.SUCCESS);
        else    Notification.show("Failed to update name.", Notification.Type.ERROR);
    }

    @FXML
    private void savePassword() {
        String current = currentPassword.getText();
        String next    = newPassword.getText();
        String confirm = confirmPassword.getText();

        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            Notification.showStrict("All fields are required", Notification.Type.INFO);
            return;
        }
        if (!next.equals(confirm)) {
            Notification.showStrict("New passwords do not match", Notification.Type.ERROR);
            return;
        }

        boolean ok = Database.updatePassword(Session.getCurrentUserId(), current, next);
        if (ok) {
            currentPassword.clear();
            newPassword.clear();
            confirmPassword.clear();
            Notification.show("Password changed!", Notification.Type.SUCCESS);
        } else {
            Notification.showStrict("Current Password is Incorrect", Notification.Type.ERROR);
        }
    }
}
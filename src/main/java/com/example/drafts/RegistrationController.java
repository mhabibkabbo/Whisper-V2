package com.example.drafts;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import static com.example.drafts.PassHasher.hashPassword;

public class RegistrationController {

    @FXML
    private TextField passwordField;

    @FXML
    private TextField confirmPasswordField;

    @FXML
    private TextField usernameField;

    @FXML
    private TextField nameField;

    @FXML
    private Label warningLabel;
    private File selectedImageFile = null;
    @FXML
    private void handleRegister() {

        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String name = nameField.getText();

        warningLabel.setText("");

        if (name.trim().isEmpty() ||
                username.trim().isEmpty() ||
                password.trim().isEmpty() ||
                confirmPassword.trim().isEmpty()) {

            warningLabel.setText("Please fill up all the fields!");
            return;
        }

        if (!password.equals(confirmPassword)) {
            warningLabel.setText("Passwords do not match!");
            return;
        }
        String hashedPass = hashPassword(password);
        byte[] imageBytes = null;

        if (selectedImageFile != null) {
            try {
                imageBytes = ImageHandler.processProfileImage(selectedImageFile);
            } catch (IOException e) {
                warningLabel.setText("Invalid image file!");
                return;
            }
        }

        boolean success = RemoteApi.insertUser(name, username, hashedPass, imageBytes);
        if(!success){
            warningLabel.setText("Username already exists!");
            return;
        }
        warningLabel.setText("Registration successful!");
        nameField.clear();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        selectedImageFile = null;
    }

    @FXML
    private void handleChooseImage() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) usernameField.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);
        if (selectedImageFile != null && selectedImageFile.length() > 2 * 1024 * 1024) {
            warningLabel.setText("Image too large! Max 2MB allowed.");
            selectedImageFile = null;
        }
    }
}

package com.example.drafts.controllers;

import com.example.drafts.ImageHandler;
import com.example.drafts.RemoteApi;
import com.example.drafts.SceneManager;
import com.example.drafts.Session;
import com.example.drafts.utils.Animations;
import com.example.drafts.utils.Notification;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

import static com.example.drafts.PassHasher.hashPassword;

public class LoginSignup {
    @FXML private TextField usernameFieldLogin;
    @FXML private PasswordField passwordFieldLogin;
    @FXML private TextField nameFieldSignup;
    @FXML private TextField usernameFieldSignup;
    @FXML private PasswordField passwordFieldSignup;
    @FXML private PasswordField confirmPasswordFieldSignup;
    @FXML private StackPane root;
    @FXML private VBox loginPanel;
    @FXML private VBox signupPanel;
    @FXML private AnchorPane container;
    @FXML private ImageView profilePreview;

    // TODO: Make Notification Popup

    private File selectedImageFile;

    public void initialize() {
        Animations.fadeIn(container);
        signupPanel.setVisible(false);
    }

    @FXML
    private void handlePickImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = fileChooser.showOpenDialog(profilePreview.getScene().getWindow());

        if (file != null) {
            if (file.length() > 2 * 1024 * 1024) {
                Notification.show("Image too large! Max 2MB allowed.", Notification.Type.ERROR);
                return;
            }

            selectedImageFile = file;
            Image image = new Image(file.toURI().toString());

            double size = 125;
            double imgW = image.getWidth();
            double imgH = image.getHeight();
            double cropSize = Math.min(imgW, imgH);
            double x = (imgW - cropSize) / 2;
            double y = (imgH - cropSize) / 2;

            profilePreview.setImage(image);
            profilePreview.setFitWidth(size);
            profilePreview.setFitHeight(size);
            profilePreview.setPreserveRatio(false);
            profilePreview.setViewport(new javafx.geometry.Rectangle2D(x, y, cropSize, cropSize));

            Circle clip = new Circle(size / 2, size / 2, size / 2);
            profilePreview.setClip(clip);
        }
    }

    @FXML
    private void onSignUpLink() {
        double width = root.getScene().getWidth();
        signupPanel.setVisible(true);
        Animations.offsetIn(signupPanel, width, 1);
        Animations.offsetOut(loginPanel, width, -1, () -> loginPanel.setVisible(false));
    }

    @FXML
    private void onLoginLink() {
        double width = root.getScene().getWidth();
        loginPanel.setVisible(true);
        Animations.offsetIn(loginPanel, width, -1);
        Animations.offsetOut(signupPanel, width, 1, () -> signupPanel.setVisible(false));
    }

    @FXML
    private void onLogin() throws IOException {
        String username = usernameFieldLogin.getText();
        String password = passwordFieldLogin.getText();

        if (username.isEmpty() || password.isEmpty()) {
            Notification.show("Fields cannot be empty!", Notification.Type.INFO);
            return;
        }

        int userId = RemoteApi.login(username, password);

        if (userId != -1) {
            Notification.show("Login Successful!", Notification.Type.SUCCESS);
            Session.setCurrentUserId(userId);
            SceneManager.switchTo("app_view.fxml", "app_view.css");
        } else {
            Notification.show("Invalid username or password", Notification.Type.ERROR);
        }
    }

    @FXML
    private void onSignUp() throws IOException {
        String username = usernameFieldSignup.getText();
        String password = passwordFieldSignup.getText();
        String confirmPassword = confirmPasswordFieldSignup.getText();
        String name = nameFieldSignup.getText();

        if (name.trim().isEmpty() ||
                username.trim().isEmpty() ||
                password.trim().isEmpty() ||
                confirmPassword.trim().isEmpty()) {

            Notification.show("Please fill up all the fields!", Notification.Type.INFO);
            return;
        }

        if (!password.equals(confirmPassword)) {
            Notification.show("Passwords do not match!", Notification.Type.ERROR);
            return;
        }
        String hashedPass = hashPassword(password);
        byte[] imageBytes = null;

        if (selectedImageFile != null) {
            try {
                imageBytes = ImageHandler.processProfileImage(selectedImageFile);
            } catch (IOException e) {
                Notification.show("Invalid image file!", Notification.Type.ERROR);
                return;
            }
        }

        boolean success = RemoteApi.insertUser(name, username, hashedPass, imageBytes);
        if(!success){
            Notification.show("Username already exists!", Notification.Type.ERROR);
            return;
        }
        Notification.show("Registration successful!\nPlease login with you credentials", Notification.Type.SUCCESS);
        nameFieldSignup.clear();
        usernameFieldSignup.clear();
        passwordFieldSignup.clear();
        confirmPasswordFieldSignup.clear();
        selectedImageFile = null;
        onLoginLink();
    }
}

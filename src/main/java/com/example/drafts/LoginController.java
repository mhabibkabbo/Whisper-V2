package com.example.drafts;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import static com.example.drafts.Database.*;

public class LoginController {
    public int id;
    @FXML
    private Button loginButton;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    public void initialize()
    {
        createMessageTable();
        createUserTable();
        createConversationTable();
        createGroupTables();
    }
    @FXML
    public void switchToRegistrationScene(javafx.event.ActionEvent actionEvent) throws IOException {
        Scene scene;
        Stage stage;
        Parent root = FXMLLoader.load(getClass().getResource("registrationScene.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    private void handleLogin() throws IOException {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Fields cannot be empty!");
            return;
        }

        int userId = login(username, password);

        if (userId != -1) {
            System.out.println("Login successful!");
            Session.setCurrentUserId(userId);
            switchToMessageScene();
        } else {
            System.out.println("Invalid username or password.");
        }
    }
    @FXML
    public void switchToMessageScene() throws IOException {

        FXMLLoader loader = new FXMLLoader(getClass().getResource("messageScene.fxml"));
        Parent root = loader.load();
        Stage stage = (Stage) loginButton.getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.show();
    }
}

package com.example.drafts;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.example.drafts.Database.getUsernameById;

public class GroupChatController {

    @FXML
    public TextField groupNameField;
    @FXML
    public VBox searchResults;
    @FXML
    public TextField searchField;
    @FXML
    public HBox selectedMembersBox;
    private Set<String> selectedMembers = new LinkedHashSet<>();
    @FXML
    public void handleSearch(){
        searchResults.getChildren().clear();
        searchResults.setAlignment(Pos.TOP_LEFT);

        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            hideSearchResults();
            return;
        }

        List<String> users = Database.searchUsers(searchText);
        boolean anyValidUserFound = false;

        for (String username : users) {
            if (username.equals(Controller.currentClient.getUsername())) {
                continue;
            }

            anyValidUserFound = true;

            Button userButton = new Button(username);
            userButton.getStyleClass().add("Search");
            userButton.setMaxWidth(Double.MAX_VALUE);

            userButton.setOnAction(e -> {
                if (selectedMembers.contains(username)) {
                    selectedMembers.remove(username);
                } else {
                    selectedMembers.add(username);
                }
                updateSelectedMembersUI();

                if (selectedMembers.contains(username)) {
                    userButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                } else {
                    userButton.setStyle("");
                }
                searchField.clear();
                hideSearchResults();
            });

            if (selectedMembers.contains(username)) {
                userButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            }

            searchResults.getChildren().add(userButton);
        }

        if (!anyValidUserFound) {
            Label notFound = new Label("No users found!");
            searchResults.setAlignment(Pos.CENTER);
            searchResults.getChildren().add(notFound);
        }

        showSearchResults();
    }

    private void showSearchResults() {

        searchResults.setVisible(true);
        searchResults.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(200), searchResults);
        fade.setFromValue(0);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(200), searchResults);
        slide.setFromY(-10);
        slide.setToY(0);

        ParallelTransition animation = new ParallelTransition(fade, slide);
        animation.play();
    }

    private void hideSearchResults() {

        FadeTransition fade = new FadeTransition(Duration.millis(150), searchResults);
        fade.setFromValue(1);
        fade.setToValue(0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(150), searchResults);
        slide.setFromY(0);
        slide.setToY(-10);

        ParallelTransition animation = new ParallelTransition(fade, slide);

        animation.setOnFinished(e -> {
            searchResults.setVisible(false);
            searchResults.setManaged(false);
        });

        animation.play();
    }

    private void updateSelectedMembersUI() {
        selectedMembersBox.getChildren().clear();
        for (String member : selectedMembers) {
            HBox chip = createMemberChip(member);
            selectedMembersBox.getChildren().add(chip);
        }
    }

    private HBox createMemberChip(String username) {
        Label nameLabel = new Label(username);
        nameLabel.setStyle("-fx-font-size: 12px;");

        Button removeBtn = new Button("✕");
        removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: gray; " +
                        "-fx-cursor: hand; -fx-padding: 0 2 0 4;"
        );
        removeBtn.setOnAction(e -> {
            selectedMembers.remove(username);
            updateSelectedMembersUI();
            if (!searchField.getText().isEmpty()) handleSearch();
        });

        HBox chip = new HBox(4, nameLabel, removeBtn);
        chip.setStyle(
                "-fx-background-color: #e0e0e0; -fx-background-radius: 12; " +
                        "-fx-padding: 4 8; -fx-alignment: center-left;"
        );
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    // Group creation
    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty() || selectedMembers.isEmpty()) {

            return;
        }
        String myUsername = Database.getUsernameById(Session.getCurrentUserId());
        int groupId = Database.createGroup(groupName, myUsername, new ArrayList<>(selectedMembers));
        if (groupId != -1) {
            Controller.currentClient.sendMessage("CREATE_GROUP|" + groupId + "|" + myUsername
                    + "|" + String.join(",", selectedMembers));
            groupNameField.clear();
            selectedMembers.clear();
            updateSelectedMembersUI();
        }
    }


    // Scene Switching ...
    @FXML
    public void switchToLoginScene(javafx.event.ActionEvent actionEvent) throws IOException {
        Scene scene;
        Stage stage;
        Parent root = FXMLLoader.load(getClass().getResource("login.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
    @FXML
    public void switchToMessageScene(javafx.event.ActionEvent actionEvent) throws IOException{
        Scene scene;
        Stage stage;
        Parent root = FXMLLoader.load(getClass().getResource("messageScene.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

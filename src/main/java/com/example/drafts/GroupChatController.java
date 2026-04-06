package com.example.drafts;

import com.example.drafts.utils.Animations;
import com.example.drafts.utils.Notification;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupChatController {

    @FXML public TextField groupNameField;
    @FXML public VBox searchResults;
    @FXML public TextField searchField;
    @FXML private HBox selectedMembersBox;
    @FXML private StackPane root;

    private final Set<String> selectedMembers = new LinkedHashSet<>();

    public void initialize() {
        Animations.fadeIn(root);
    }

    @FXML
    public void handleSearch(){
        searchResults.getChildren().clear();
        searchResults.setAlignment(Pos.TOP_LEFT);

        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            hideSearchResults();
            return;
        }

        List<String> users = RemoteApi.searchUsers(searchText);
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
                    userButton.setStyle("-fx-background-color: #3d8b5e; -fx-text-fill: white;");
                } else {
                    userButton.setStyle("");
                }
                searchField.clear();
                hideSearchResults();
            });

            if (selectedMembers.contains(username)) {
                userButton.setStyle("-fx-background-color: #3d8b5e; -fx-text-fill: white;");
            }

            searchResults.getChildren().add(userButton);
        }

        if (!anyValidUserFound) {
            Label notFound = new Label("No users found!");
            notFound.getStyleClass().add("title-2");
            searchResults.setAlignment(Pos.CENTER);
            searchResults.getChildren().add(notFound);
        }

        showSearchResults();
    }

    private void showSearchResults() {
        searchResults.setVisible(true);
        searchResults.setManaged(true);

        Animations.parallel(
                Animations.fade(searchResults, 200, 0, 1, null),
                Animations.translateY(searchResults, 200, -20, 0, null)
        ).play();
    }

    private void hideSearchResults() {
        Animations.parallel(
                Animations.fade(searchResults, 200, 1, 0, null),
                Animations.translateY(searchResults, 200, 0, -20, () -> {
                    searchResults.setVisible(false);
                    searchResults.setManaged(false);
                })
        ).play();
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
        nameLabel.getStyleClass().add("chip-label");

        Button removeBtn = new Button("❌");
        removeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: gray; " +
                "-fx-cursor: hand; -fx-padding: 5 8 5 3;"
        );
        removeBtn.setOnAction(e -> {
            selectedMembers.remove(username);
            updateSelectedMembersUI();
            if (!searchField.getText().isEmpty()) handleSearch();
        });

        HBox chip = new HBox(3, nameLabel, removeBtn);
        chip.getStyleClass().add("chip");
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    // Group creation
    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();
        if (groupName.isEmpty() || selectedMembers.isEmpty()) {
            Notification.show("Group Name or Members cannot be empty!", Notification.Type.INFO);
            return;
        }
        String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
        int groupId = RemoteApi.createGroup(groupName, myUsername, new ArrayList<>(selectedMembers));
        if (groupId != -1) {
            Controller.currentClient.sendMessage("CREATE_GROUP|" + groupId + "|" + myUsername
                    + "|" + String.join(",", selectedMembers));
            Notification.show("Group created!", Notification.Type.SUCCESS);
            groupNameField.clear();
            selectedMembers.clear();
            updateSelectedMembersUI();
        }
    }
}

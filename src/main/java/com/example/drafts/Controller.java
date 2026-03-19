package com.example.drafts;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.drafts.Database.*;

public class Controller {
    @FXML
    private Text profileName;
    @FXML
    private VBox messageContainer;
    @FXML
    private TextField searchField;
    @FXML
    private VBox searchResults;
    @FXML
    private TextArea messageField;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private ImageView currentChatUserPicture;
    @FXML
    private VBox conversationListVBox;
    @FXML
    private ScrollPane conversationListScroll;
    public static Client currentClient;
    private String currentChatGroupName;
    Map<Integer, String> groupNames = new HashMap<>();
    public void initialize()
    {
        loadConversations();
        profileName.setText("Friends Forever");
        currentClient = new Client(getUsernameById(Session.getCurrentUserId()), this);
        searchResults.setVisible(false);
        searchResults.setManaged(false);
        searchResults.setOpacity(0);
        searchResults.setTranslateY(-10);
        messageContainer.setOnMouseClicked(e -> hideSearchResults());
    }
    @FXML
    private void handleSend() {
            String message = messageField.getText();
            if (message.trim().isEmpty()) return;
            int groupId = Session.getCurrentGroupId();

        if (groupId != -1) {
            try {
                currentClient.sendGroupMessage(groupId, message);
            } catch (IOException e) { e.printStackTrace(); }
            String myUsername = Database.getUsernameById(Session.getCurrentUserId());
            Database.saveGroupMessage(groupId, myUsername, message);
            addMessage(message, true);
            addOrUpdateConversation("group:" + Session.getCurrentGroupId(), myUsername + "|" + message);

        } else if (currentChatUser != null) {
                currentClient.sendMessage(
                        "PM|" + currentChatUser + "|" + message
                );
                int receiverId = getUserIdByUsername(currentChatUser);
                addMessage(message, true, receiverId);
                addOrUpdateConversation("dm:" + currentChatUser, message);
                int myId = Session.getCurrentUserId();
                String myUsername = getUsernameById(Session.getCurrentUserId());
                Database.upsertConversation(myUsername, currentChatUser, message);
                Database.saveMessage(myId, receiverId, message);
            }
            messageField.clear();
    }
    public void addMessage(String message, boolean isMyMessage) {
        addMessage(message, isMyMessage, -1);
    }
    public void addMessage(String message, boolean isMyMessage, int receiverID) {

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(300);
        messageLabel.setFont(Font.font(14));
        messageLabel.setPadding(new Insets(8));

        ImageView profileImageView = new ImageView();

        if (isMyMessage) {
            profileImageView.setImage(ImageHandler.getProfileImage(Session.getCurrentUserId()));
            messageLabel.getStyleClass().add("my-message");
        } else {
            profileImageView.setImage(ImageHandler.getProfileImage(receiverID));
            messageLabel.getStyleClass().add("friend-message");
        }

        profileImageView.setFitWidth(30);
        profileImageView.setFitHeight(30);
        profileImageView.setPreserveRatio(true);
        Circle clip = new Circle(15, 15, 15);
        profileImageView.setClip(clip);

        HBox messageBox = new HBox(8);
        messageBox.setPadding(new Insets(5));

        if (isMyMessage) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().addAll(messageLabel, profileImageView);
        } else {
            // Show sender name above bubble only in group chat
            if (Session.getCurrentGroupId() != -1 && receiverID != -1) {
                String senderName = Database.getUsernameById(receiverID);
                Label senderLabel = new Label(senderName);
                senderLabel.setStyle(
                        "-fx-font-size: 11px; -fx-text-fill: gray; -fx-padding: 0 0 2 38;"
                );
                messageContainer.getChildren().add(senderLabel); // add name row first
            }
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.getChildren().addAll(profileImageView, messageLabel);
        }

        messageContainer.getChildren().add(messageBox);
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }


    public void receiveMessage(String message) {
        addMessage(message, false, -1);
    }

    private HBox createConversationCell(String username, String lastMessage, Image profilePic) {

        ImageView profileImageView = new ImageView(profilePic);
        profileImageView.setFitWidth(40);
        profileImageView.setFitHeight(40);
        profileImageView.setPreserveRatio(true);

        Circle clip = new Circle(20, 20, 20);
        profileImageView.setClip(clip);

        Label nameLabel = new Label(username);
        nameLabel.setStyle(
                "-fx-font-size: 13px; -fx-font-family: \"JetBrains Mono Medium\";"
        );

        Label lastMsgLabel = new Label(lastMessage);
        lastMsgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");

        VBox textVBox = new VBox(nameLabel, lastMsgLabel);
        textVBox.setSpacing(2);

        HBox hbox = new HBox(10, profileImageView, textVBox);
        hbox.setPadding(new Insets(5));
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.setOnMouseClicked(e -> openPrivateChat(username));
        hbox.setUserData("dm:" + username);
        return hbox;
    }

    private void loadConversations() {
        String myUsername = getUsernameById(Session.getCurrentUserId());
        List<Conversation> conversations = Database.getConversations(myUsername);

        conversationListVBox.getChildren().clear();

        for (Conversation conv : conversations) {

            String otherUser = conv.getOtherUser(myUsername);
            String lastMsg = conv.getLastMessage();
            Image profilePicture = ImageHandler.getProfileImage(getUserIdByUsername(otherUser));
            HBox cell = createConversationCell(otherUser, lastMsg, profilePicture);

            conversationListVBox.getChildren().add(cell);
        }

        List<String[]> groups = Database.getGroupsForUser(myUsername);
        for (String[] g : groups) {
            int groupId = Integer.parseInt(g[0]);
            String groupName = g[1];
            groupNames.put(groupId, groupName);
            HBox cell = createGroupConversationCell(groupId, groupName, "");
            conversationListVBox.getChildren().add(cell);
        }
    }

    private HBox createGroupConversationCell(int groupId, String groupName, String lastMsg) {
        Label nameLabel = new Label("👥 " + groupName);
        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-family: \"JetBrains Mono Medium\";");
        Label lastMsgLabel = new Label(lastMsg);
        lastMsgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        VBox textVBox = new VBox(nameLabel, lastMsgLabel);
        textVBox.setSpacing(2);
        HBox hbox = new HBox(10, textVBox);
        hbox.setPadding(new Insets(5));
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setUserData("group:" + groupId);
        hbox.setOnMouseClicked(e -> openGroupChat(groupId, groupName));
        return hbox;
    }

    private void addOrUpdateConversation(String key, String lastMessage) {
        HBox existingCell = null;

        for (Node node : conversationListVBox.getChildren()) {
            HBox hbox = (HBox) node;
            Object tag = hbox.getUserData();
            if (tag != null && tag.toString().equals(key)) {
                for (Node child : hbox.getChildren()) {
                    if (child instanceof VBox textVBox) {
                        if (key.startsWith("group:") && lastMessage.contains("|")) {
                            String[] parts = lastMessage.split("\\|", 2);
                            Label previewLabel = (Label) textVBox.getChildren().get(1);
                            previewLabel.setText(parts[0] + ": " + parts[1]); // "bappy: hello"
                            break;
                        } else {
                            Label previewLabel = (Label) textVBox.getChildren().get(1);
                            previewLabel.setText(lastMessage);
                        }
                        break;
                    }
                }
                existingCell = hbox;
                break;
            }
        }

        if (existingCell != null) {
            conversationListVBox.getChildren().remove(existingCell);
            conversationListVBox.getChildren().add(0, existingCell);
        } else if (key.startsWith("dm:")) {
            String username = key.replace("dm:", "");
            Image profilePic = ImageHandler.getProfileImage(Database.getUserIdByUsername(username));
            HBox newCell = createConversationCell(username, lastMessage, profilePic);
            conversationListVBox.getChildren().add(0, newCell);
        }
    }
    private String currentChatUser;
    @FXML
    private void handleSearch() {
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
            if (username.equals(currentClient.getUsername())) {
                continue;
            }

            anyValidUserFound = true;

            Button userButton = new Button(username);
            userButton.getStyleClass().add("Search");
            userButton.setMaxWidth(Double.MAX_VALUE);

            userButton.setOnAction(e -> {
                openPrivateChat(username);
                searchField.clear();
                hideSearchResults();
            });

            searchResults.getChildren().add(userButton);
        }

        if (!anyValidUserFound) {
            Label notFound = new Label("No users found!");
            searchResults.setAlignment(Pos.CENTER);
            searchResults.getChildren().add(notFound);
        }

        showSearchResults();
    }

    private void openPrivateChat(String username) {
        Session.setCurrentGroupId(-1);
        currentChatUser = username;
        profileName.setText(getNameById(getUserIdByUsername(username)));
        currentChatUserPicture.setImage(ImageHandler.getProfileImage(getUserIdByUsername(username)));
        Circle clip = new Circle(20, 20, 20);
        currentChatUserPicture.setClip(clip);
        messageContainer.getChildren().clear();

        int myId = Session.getCurrentUserId();
        int otherId = getUserIdByUsername(username);
        List<Message> history = Database.getMessagesBetween(myId, otherId);

        for (Message msg : history) {
            boolean isMe = msg.getSenderId() == myId;
            addMessage(msg.getContent(), isMe, otherId);
        }
    }

    private void openGroupChat(int groupId, String groupName) {
        Session.setCurrentGroupId(groupId);
        currentChatUser = null;
        currentChatGroupName = groupName;
        profileName.setText(groupName);
       // currentChatUserPicture.setImage();
        messageContainer.getChildren().clear();

        List<GroupMessage> history = Database.getGroupMessages(groupId);
        String myUsername = Database.getUsernameById(Session.getCurrentUserId());
        for (GroupMessage gm : history) {
            boolean isMe = gm.getSenderUsername().equals(myUsername);
            int senderId = isMe ? Session.getCurrentUserId() : Database.getUserIdByUsername(gm.getSenderUsername());
            addMessage(gm.getContent(), isMe, senderId);
        }
    }

    public void receiveGroupMessage(int groupId, String sender, String message) {
        String myUsername = getUsernameById(Session.getCurrentUserId());
        if (!sender.equals(myUsername)) {
            Database.saveGroupMessage(groupId, sender, message);
        }

        if (groupId == Session.getCurrentGroupId()) {
            addMessage(message, false, Database.getUserIdByUsername(sender));
        }

        String groupName = groupNames.get(groupId);
        if (groupName != null) {
            addOrUpdateConversation("group:" + groupId, sender + "|" + message);
        }
    }
    public void receivePrivateMessage(String sender, String message) {

        String myUsername = getUsernameById(Session.getCurrentUserId());

        if (sender.equals(currentChatUser)) {
            addMessage(message, false, getUserIdByUsername(sender));
            Database.saveMessage(getUserIdByUsername(sender), Session.getCurrentUserId(), message);
            Database.upsertConversation(sender, myUsername, message);
            addOrUpdateConversation(sender, message);
        }
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
    public void switchToGroupChatScene(javafx.event.ActionEvent actionEvent) throws IOException {
        Scene scene;
        Stage stage;
        Parent root = FXMLLoader.load(getClass().getResource("GroupChat.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene = new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}

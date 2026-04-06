package com.example.drafts;

import com.example.drafts.controllers.Settings;
import com.example.drafts.utils.Animations;
import com.example.drafts.utils.ChatBackground;
import com.example.drafts.utils.Notification;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {
    @FXML private Button callButton;
    @FXML
    private VBox chatBackground;
    @FXML
    private VBox searchContainer;
    @FXML
    private HBox root;
    @FXML
    private StackPane leftPanel;
    @FXML
    private Label profileName;
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

    // Attachment state
    private byte[] pendingAttachmentData;
    private String pendingAttachmentName;
    private String pendingAttachmentMime;
    @FXML
    private VBox attachmentPreviewBar;   // hidden by default (managed=false)
    @FXML
    private Label attachmentPreviewLabel;
    @FXML
    private ImageView attachmentPreviewThumb;

    // Voice call state
    private VoiceCall activeCall;
    private String callPeer;        // username of the other party
    private int pendingCallerUdpPort; // stored on incoming call until accepted
    private Timeline callTimer;
    private int callSeconds;
    private int pendingLocalUdpPort;
    @FXML
    private HBox incomingCallBanner;
    @FXML
    private ImageView callerPicBanner;
    @FXML
    private Label callerNameBanner;
    private Clip ringtoneClip;

    @FXML
    private VBox callScreen;
    @FXML
    private ImageView callScreenPic;
    @FXML
    private Label callScreenName;
    @FXML
    private Label callDurationLabel;
    @FXML
    private Button muteBtn;

    public static Client currentClient;
    private String firstContact;
    private String currentChatGroupName;
    private static Controller instance;
    Map<Integer, String> groupNames = new HashMap<>();
    private boolean loadingPrivateHistory;
    private boolean loadingGroupHistory;

    public void initialize() {
        instance = this;
        loadConversations();
        profileName.setText("Will remove");
        currentClient = new Client(RemoteApi.getUsernameById(Session.getCurrentUserId()), this);
        searchContainer.setVisible(false);
        searchContainer.setManaged(false);
        searchContainer.setOpacity(0);
        scrollPane.setOnMouseClicked(e -> hideSearchResults());

        leftPanel.prefWidthProperty().bind(
                Bindings.min(400, root.widthProperty().multiply(0.40))
        );

        messageField.setOnKeyPressed(e -> {
            if (Settings.getBool("enterToSend", false)) {
                if (e.getCode() == KeyCode.ENTER) {
                    if (e.isShiftDown()) {
                        int pos = messageField.getCaretPosition();
                        messageField.insertText(pos, "\n");
                    } else {
                        handleSend();
                    }
                    e.consume();
                }
            }
        });

        applyMessageSpacing();
        Animations.fadeIn(root);
        applyBackground();
    }

    public static void applySettingsLive() {
        if (instance == null) return;
        instance.applyMessageSpacing();
        instance.applyBackground();
    }

    private double getMessageFontSize() {
        String raw = Settings.get("fontSize", "15px").replace("px", "").trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            return 15.0;
        }
    }

    private void applyMessageSpacing() {
        String raw = Settings.get("messageSpacing", "5px").replace("px", "").trim();
        try {
            messageContainer.setSpacing(Double.parseDouble(raw));
        } catch (NumberFormatException e) {
            messageContainer.setSpacing(5.0);
        }
    }

    private void applyBackground() {
        ChatBackground bg = ChatBackground.fromKey(Settings.get("chatBackground", "DEFAULT"));

        for (ChatBackground b : ChatBackground.values())
            if (b.cssClass != null)
                chatBackground.getStyleClass().remove(b.cssClass);

        if (bg.cssClass != null)
            chatBackground.getStyleClass().add(bg.cssClass);
    }

    @FXML
    private void handleSend() {
        if (pendingAttachmentData != null) {
            byte[] data = pendingAttachmentData;
            String name = pendingAttachmentName;
            String mime = pendingAttachmentMime;
            String caption = messageField.getText().trim();
            clearAttachment();

            int groupId = Session.getCurrentGroupId();
            if (groupId != -1) {
                try {
                    currentClient.sendGroupFile(groupId, name, mime, data);
                    String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
                    addAttachmentMessage(name, mime, data, true, Session.getCurrentUserId());
                    addOrUpdateConversation("group:" + groupId, myUsername + "|📎 " + name);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else if (currentChatUser != null) {
                try {
                    currentClient.sendPrivateFile(currentChatUser, name, mime, data);
                    int myId = Session.getCurrentUserId();
                    addAttachmentMessage(name, mime, data, true, myId);
                    addOrUpdateConversation("dm:" + currentChatUser, "📎 " + name);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (!caption.isEmpty()) {
                handleSend();
                return;
            }
            messageField.clear();
            return;
        }

        String message = messageField.getText();
        if (message.trim().isEmpty()) return;
        int groupId = Session.getCurrentGroupId();

        if (groupId != -1) {
            try {
                currentClient.sendGroupMessage(groupId, message);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
            addMessage(message, true);
            addOrUpdateConversation("group:" + Session.getCurrentGroupId(), myUsername + "|" + message);

        } else if (currentChatUser != null) {
            currentClient.sendMessage("PM|" + currentChatUser + "|" + message);
            int receiverId = RemoteApi.getUserIdByUsername(currentChatUser);
            addMessage(message, true, receiverId);
            addOrUpdateConversation("dm:" + currentChatUser, message);
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
        messageLabel.setPadding(new Insets(5));
        messageLabel.setStyle("-fx-font-size: " + getMessageFontSize() + "px;"); // ← font size

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

        HBox messageBox = new HBox(5);
        messageBox.setPadding(new Insets(2.5));

        if (isMyMessage) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
            messageBox.getChildren().addAll(messageLabel, profileImageView);
        } else {
            if (Session.getCurrentGroupId() != -1 && receiverID != -1) {
                String senderName = RemoteApi.getUsernameById(receiverID);
                Label senderLabel = new Label(senderName);
                senderLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray; -fx-padding: 0 0 -2 38;");
                messageContainer.getChildren().add(senderLabel);
            }
            messageBox.setAlignment(Pos.CENTER_LEFT);
            messageBox.getChildren().addAll(profileImageView, messageLabel);
        }

        messageContainer.getChildren().add(messageBox);

        if (Settings.getBool("autoScroll", true)) {
            Platform.runLater(() -> {
                messageContainer.layout();
                scrollPane.layout();
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            });
        }
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
                "-fx-font-size: 15px;"
        );

        Label lastMsgLabel = new Label(lastMessage);
        lastMsgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        lastMsgLabel.setWrapText(false);
        lastMsgLabel.setPrefHeight(15);
        lastMsgLabel.setMaxHeight(Control.USE_PREF_SIZE);

        VBox textVBox = new VBox(nameLabel, lastMsgLabel);
        textVBox.setSpacing(2);

        HBox hbox = new HBox(10, profileImageView, textVBox);
        hbox.setStyle("-fx-background-color: transparent;");
        hbox.setPadding(new Insets(5));
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.setOnMouseClicked(e -> openPrivateChat(username));
        hbox.setOnMouseEntered(e -> hbox.setStyle("-fx-background-color: #25343f19; -fx-background-radius: 10px;"));
        hbox.setOnMouseExited(e -> hbox.setStyle("-fx-background-color: transparent;"));
        hbox.setUserData("dm:" + username);
        return hbox;
    }

//    private void loadConversations() {
//        String myUsername = getUsernameById(Session.getCurrentUserId());
//        List<Conversation> conversations = Database.getConversations(myUsername);
//
//        conversationListVBox.getChildren().clear();
//
//        for (Conversation conv : conversations) {
//
//            String otherUser = conv.getOtherUser(myUsername);
//            String lastMsg = conv.getLastMessage();
//            Image profilePicture = ImageHandler.getProfileImage(getUserIdByUsername(otherUser));
//            HBox cell = createConversationCell(otherUser, lastMsg, profilePicture);
//
//            conversationListVBox.getChildren().add(cell);
//        }
//
//        List<String[]> groups = Database.getGroupsForUser(myUsername);
//        for (String[] g : groups) {
//            int groupId = Integer.parseInt(g[0]);
//            String groupName = g[1];
//            groupNames.put(groupId, groupName);
//            HBox cell = createGroupConversationCell(groupId, groupName, "");
//            conversationListVBox.getChildren().add(cell);
//        }
//    }

    private void loadConversations() {
        String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
        List<Object[]> allEntries = new ArrayList<>();

        for (Conversation conv : RemoteApi.getConversations(myUsername)) {
            String otherUser  = conv.getOtherUser(myUsername);
            String lastMsg    = conv.getLastMessage();
            String timestamp  = conv.getLastTimestamp();

            Image  pic        = ImageHandler.getProfileImage(RemoteApi.getUserIdByUsername(otherUser));
            HBox   cell       = createConversationCell(otherUser, lastMsg, pic);
            allEntries.add(new Object[]{ cell, timestamp != null ? timestamp : "1970-01-01 00:00:00" });
        }

        for (String[] g : RemoteApi.getGroupsForUser(myUsername)) {
            int    groupId   = Integer.parseInt(g[0]);
            String groupName = g[1];
            groupNames.put(groupId, groupName);

            String preview   = "";
            String timestamp = "1970-01-01 00:00:00";

            String[] last = RemoteApi.getLastGroupMessage(groupId);
            if (last != null) {
                preview   = last[0] + ": " + last[1];
                timestamp = last[2];
            }

            HBox cell = createGroupConversationCell(groupId, groupName, preview);
            allEntries.add(new Object[]{ cell, timestamp });
        }
        allEntries.sort((a, b) -> ((String) b[1]).compareTo((String) a[1]));

        conversationListVBox.getChildren().clear();
        for (Object[] entry : allEntries)
            conversationListVBox.getChildren().add((HBox) entry[0]);
    }

    private HBox createGroupConversationCell(int groupId, String groupName, String lastMsg) {
        ImageView profileImageView = new ImageView(
                new Image(
                        SceneManager.class.getResource("icons/group.png").toExternalForm()
                )
        );
        profileImageView.setFitWidth(40);
        profileImageView.setFitHeight(35);
        profileImageView.setPreserveRatio(true);

        Label nameLabel = new Label(groupName);
        nameLabel.setStyle("-fx-font-size: 15px;");

        Label lastMsgLabel = new Label(lastMsg);
        lastMsgLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        lastMsgLabel.setWrapText(false);
        lastMsgLabel.setPrefHeight(15);
        lastMsgLabel.setMaxHeight(Control.USE_PREF_SIZE);

        VBox textVBox = new VBox(nameLabel, lastMsgLabel);
        textVBox.setSpacing(2);

        HBox hbox = new HBox(10, profileImageView, textVBox);
        hbox.setStyle("-fx-background-color: transparent;");
        hbox.setPadding(new Insets(5));
        hbox.setAlignment(Pos.CENTER_LEFT);

        hbox.setOnMouseEntered(e -> hbox.setStyle("-fx-background-color: #25343f19; -fx-background-radius: 10px;"));
        hbox.setOnMouseExited(e -> hbox.setStyle("-fx-background-color: transparent;"));
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
            Image profilePic = ImageHandler.getProfileImage(RemoteApi.getUserIdByUsername(username));
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

        List<String> users = RemoteApi.searchUsers(searchText);
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
        callButton.setVisible(true);
        callButton.setManaged(true);

        Session.setCurrentGroupId(-1);
        currentChatUser = username;
        profileName.setText(RemoteApi.getNameById(RemoteApi.getUserIdByUsername(username)));
        currentChatUserPicture.setImage(ImageHandler.getProfileImage(RemoteApi.getUserIdByUsername(username)));
        Circle clip = new Circle(20, 20, 20);
        currentChatUserPicture.setClip(clip);
        messageContainer.getChildren().clear();
        loadingPrivateHistory = true;
        currentClient.requestPrivateHistory(username);
    }

    private void openGroupChat(int groupId, String groupName) {
        callButton.setVisible(false);
        callButton.setManaged(false);

        Session.setCurrentGroupId(groupId);
        currentChatUser = null;
        currentChatGroupName = groupName;
        profileName.setText(groupName);
        // currentChatUserPicture.setImage();
        messageContainer.getChildren().clear();
        loadingGroupHistory = true;
        currentClient.requestGroupHistory(groupId);
    }

//    public void receiveGroupMessage(int groupId, String sender, String message) {
//        String myUsername = getUsernameById(Session.getCurrentUserId());
//        if (!sender.equals(myUsername)) {
//            Database.saveGroupMessage(groupId, sender, message);
//        }
//
//        if (groupId == Session.getCurrentGroupId()) {
//            addMessage(message, false, Database.getUserIdByUsername(sender));
//            Notification.playSound();
//        } else {
//            String groupName = groupNames.get(groupId);
//            Notification.showMessageNotif(
//                    groupName != null ? groupName : sender, sender + ": " + message
//            );
//        }
//
//        String groupName = groupNames.get(groupId);
//        if (groupName != null) {
//            addOrUpdateConversation("group:" + groupId, sender + "|" + message);
//        }
//    }

    public void receiveGroupMessage(int groupId, String sender, String message) {
        if (groupId == Session.getCurrentGroupId()) {
            addMessage(message, false, RemoteApi.getUserIdByUsername(sender));
            Notification.playSound();
        } else {
            String groupName = groupNames.get(groupId);
            Notification.showMessageNotif(groupName != null ? groupName : sender, sender + ": " + message);
        }

        String groupName = groupNames.get(groupId);
        if (groupName != null) addOrUpdateConversation("group:" + groupId, sender + "|" + message);
    }

//    public void receivePrivateMessage(String sender, String message) {
//
//        String myUsername = getUsernameById(Session.getCurrentUserId());
//
//        if (sender.equals(currentChatUser)) {
//            addMessage(message, false, getUserIdByUsername(sender));
//            Notification.playSound();
//            Database.saveMessage(getUserIdByUsername(sender), Session.getCurrentUserId(), message);
//            Database.upsertConversation(sender, myUsername, message);
//            addOrUpdateConversation("dm:" + sender, message);
//        } else {
//            Notification.showMessageNotif(sender, message);
//        }
//    }

    public void receivePrivateMessage(String sender, String message) {
        if (sender.equals(currentChatUser)) {
            addMessage(message, false, RemoteApi.getUserIdByUsername(sender));
            Notification.playSound();
            addOrUpdateConversation("dm:" + sender, message);
        } else {
            addOrUpdateConversation("dm:" + sender, message);
            Notification.showMessageNotif(sender, message);
        }
    }

    private void showSearchResults() {
        searchContainer.setVisible(true);
        searchContainer.setManaged(true);

        conversationListScroll.setVisible(false);
        conversationListScroll.setManaged(false);

        Animations.parallel(
                Animations.fade(searchContainer, 200, 0, 1, null),
                Animations.translateY(searchContainer, 200, -20, 0, null)
        ).play();
    }

    private void hideSearchResults() {
        Animations.parallel(
                Animations.fade(searchContainer, 200, 1, 0, null),
                Animations.translateY(searchContainer, 200, 0, -20, () -> {
                    searchContainer.setVisible(false);
                    searchContainer.setManaged(false);
                })
        ).play();

        conversationListScroll.setVisible(true);
        conversationListScroll.setManaged(true);
    }

    // Attachment
    @FXML
    private void handleAttach() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Attach File");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Documents & Archives", "*.pdf", "*.txt", "*.zip", "*.docx", "*.xlsx"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File file = chooser.showOpenDialog(root.getScene().getWindow());
        if (file == null) return;

        if (file.length() > 5L * 1024 * 1024) {
            Notification.showMessageNotif("Attachment", "File too large — max 5 MB");
            return;
        }

        try {
            pendingAttachmentData = Files.readAllBytes(file.toPath());
            pendingAttachmentName = file.getName();
            pendingAttachmentMime = Files.probeContentType(file.toPath());
            if (pendingAttachmentMime == null) pendingAttachmentMime = "application/octet-stream";
            showAttachmentPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAttachmentPreview() {
        attachmentPreviewLabel.setText(pendingAttachmentName);
        if (pendingAttachmentMime.startsWith("image/")) {
            attachmentPreviewThumb.setImage(new Image(new ByteArrayInputStream(pendingAttachmentData)));
            attachmentPreviewThumb.setVisible(true);
            attachmentPreviewThumb.setManaged(true);
        } else {
            attachmentPreviewThumb.setVisible(false);
            attachmentPreviewThumb.setManaged(false);
        }
        attachmentPreviewBar.setVisible(true);
        attachmentPreviewBar.setManaged(true);
    }

    @FXML
    private void clearAttachment() {
        pendingAttachmentData = null;
        pendingAttachmentName = null;
        pendingAttachmentMime = null;
        attachmentPreviewBar.setVisible(false);
        attachmentPreviewBar.setManaged(false);
    }

    public void addAttachmentMessage(String filename, String mimeType, byte[] data,
                                     boolean isMyMessage, int profileUserId) {
        Node bubble;

        if (mimeType != null && mimeType.startsWith("image/")) {
            Image image = new Image(new ByteArrayInputStream(data));
            ImageView iv = new ImageView(image);
            double aspectRatio = image.getWidth() / image.getHeight();
            iv.setFitWidth(220);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            Rectangle clip = new Rectangle(220, 220 / aspectRatio);
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            iv.setClip(clip);

            StackPane pane = new StackPane(iv);
            pane.getStyleClass().add(isMyMessage ? "my-message" : "friend-message");
            pane.setPadding(new Insets(3));
            pane.setCursor(Cursor.HAND);
            pane.setOnMouseClicked(e -> saveFileToDisk(filename, data));
            bubble = pane;

        } else {
            Label icon = new Label("📎");
            icon.setStyle("-fx-font-size: 14px;");
            Label nameLabel = new Label(filename);
            nameLabel.setStyle(
                    "-fx-font-size: " + getMessageFontSize() + "px;" +
                    "-fx-text-fill: " + (isMyMessage ? "white;" : "-fx-primary-color;")
            );
            Button saveBtn = new Button("Save");
            saveBtn.setOnAction(e -> saveFileToDisk(filename, data));

            HBox box = new HBox(8, icon, nameLabel, saveBtn);
            box.setAlignment(Pos.CENTER_LEFT);
            box.setPadding(new Insets(8, 12, 8, 0));
            box.getStyleClass().add(isMyMessage ? "my-message" : "friend-message");
            bubble = box;
        }

        ImageView pic = new ImageView(ImageHandler.getProfileImage(profileUserId));
        pic.setFitWidth(30);
        pic.setFitHeight(30);
        pic.setPreserveRatio(true);
        pic.setClip(new Circle(15, 15, 15));

        HBox row = new HBox(5);
        row.setPadding(new Insets(2.5));
        if (isMyMessage) {
            row.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(bubble, pic);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            row.getChildren().addAll(pic, bubble);
        }
        messageContainer.getChildren().add(row);

        if (Settings.getBool("autoScroll", true)) {
            Platform.runLater(() -> {
                messageContainer.layout();
                scrollPane.layout();
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            });
        }
    }

    private void saveFileToDisk(String filename, byte[] data) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save File");
        chooser.setInitialFileName(filename);
        File dest = chooser.showSaveDialog(root.getScene().getWindow());
        if (dest == null) return;
        try {
            Files.write(dest.toPath(), data);
            Notification.showMessageNotif("Saved", dest.getName());
        } catch (IOException e) {
            e.printStackTrace();
            Notification.showMessageNotif("Error", "Could not save file");
        }
    }

//    public void receivePrivateFile(String sender, String filename, String mimeType, byte[] data) {
//        String myUsername = getUsernameById(Session.getCurrentUserId());
//        int senderId = getUserIdByUsername(sender);
//        Database.saveMessage(senderId, Session.getCurrentUserId(), "", data, filename, mimeType);
//        Database.upsertConversation(sender, myUsername, "📎 " + filename);
//
//        if (sender.equals(currentChatUser)) {
//            addAttachmentMessage(filename, mimeType, data, false, senderId);
//            addOrUpdateConversation("dm:" + sender, "📎 " + filename);
//            Notification.playSound();
//        } else {
//            Notification.showMessageNotif(sender, "📎 " + filename);
//        }
//    }

    public void receivePrivateFile(String sender, String filename, String mimeType, byte[] data) {
        int senderId = RemoteApi.getUserIdByUsername(sender);

        if (sender.equals(currentChatUser)) {
            addAttachmentMessage(filename, mimeType, data, false, senderId);
            addOrUpdateConversation("dm:" + sender, "📎 " + filename);
            Notification.playSound();
        } else {
            addOrUpdateConversation("dm:" + sender, "📎 " + filename);
            Notification.showMessageNotif(sender, "📎 " + filename);
        }
    }

//    public void receiveGroupFile(int groupId, String sender, String filename, String mimeType, byte[] data) {
//        String myUsername = getUsernameById(Session.getCurrentUserId());
//        if (!sender.equals(myUsername)) {
//            Database.saveGroupMessage(groupId, sender, "", data, filename, mimeType);
//        }
//
//        if (groupId == Session.getCurrentGroupId()) {
//            addAttachmentMessage(filename, mimeType, data, false, getUserIdByUsername(sender));
//            Notification.playSound();
//        } else {
//            String groupName = groupNames.get(groupId);
//            Notification.showMessageNotif(
//                    groupName != null ? groupName : sender, sender + ": 📎 " + filename
//            );
//        }
//
//        String groupName = groupNames.get(groupId);
//        if (groupName != null) addOrUpdateConversation("group:" + groupId, sender + "|📎 " + filename);
//    }

    public void receiveGroupFile(int groupId, String sender, String filename, String mimeType, byte[] data) {
        if (groupId == Session.getCurrentGroupId()) {
            addAttachmentMessage(filename, mimeType, data, false, RemoteApi.getUserIdByUsername(sender));
            Notification.playSound();
        } else {
            String groupName = groupNames.get(groupId);
            Notification.showMessageNotif(groupName != null ? groupName : sender, sender + ": 📎 " + filename);
        }

        String groupName = groupNames.get(groupId);
        if (groupName != null) addOrUpdateConversation("group:" + groupId, sender + "|📎 " + filename);
    }

    public void onPrivateHistoryItem(String peer, String sender, String message,
                                     String attachmentName, String attachmentType, byte[] attachmentData) {
        if (!loadingPrivateHistory) return;
        if (currentChatUser == null || !currentChatUser.equals(peer)) return;

        String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
        boolean isMe = sender.equals(myUsername);
        int profileId = isMe ? Session.getCurrentUserId() : RemoteApi.getUserIdByUsername(sender);

        if (attachmentData != null && attachmentData.length > 0) {
            addAttachmentMessage(attachmentName, attachmentType, attachmentData, isMe, profileId);
        }
        if (message != null && !message.isBlank()) {
            addMessage(message, isMe, profileId);
        }
    }

    public void onPrivateHistoryEnd(String peer) {
        if (currentChatUser != null && currentChatUser.equals(peer)) {
            loadingPrivateHistory = false;
            Platform.runLater(() -> {
                messageContainer.layout();
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });
        }
    }

    public void onGroupHistoryItem(int groupId, String sender, String message,
                                   String attachmentName, String attachmentType, byte[] attachmentData) {
        if (!loadingGroupHistory) return;
        if (Session.getCurrentGroupId() != groupId) return;

        String myUsername = RemoteApi.getUsernameById(Session.getCurrentUserId());
        boolean isMe = sender.equals(myUsername);
        int profileId = isMe ? Session.getCurrentUserId() : RemoteApi.getUserIdByUsername(sender);

        if (attachmentData != null && attachmentData.length > 0) {
            addAttachmentMessage(attachmentName, attachmentType, attachmentData, isMe, profileId);
        }
        if (message != null && !message.isBlank()) {
            addMessage(message, isMe, profileId);
        }
    }

    public void onGroupHistoryEnd(int groupId) {
        if (Session.getCurrentGroupId() == groupId) {
            loadingGroupHistory = false;
            Platform.runLater(() -> {
                messageContainer.layout();
                scrollPane.layout();
                scrollPane.setVvalue(1.0);
            });
        }
    }


    // Call Handling
    @FXML
    private void handleInitiateCall() {
        if (currentChatUser == null || activeCall != null) return;
        callPeer = currentChatUser;
        try {
            pendingLocalUdpPort = grabFreeUdpPort();
            currentClient.sendCallRequest(currentChatUser, pendingLocalUdpPort);
            showOutgoingCallScreen(currentChatUser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int grabFreeUdpPort() throws Exception {
        try (DatagramSocket s = new DatagramSocket()) {
            return s.getLocalPort();
        }
    }

    public void onIncomingCall(String caller, int callerUdpPort) {
        if (activeCall != null) {
            currentClient.sendCallReject(caller);
            return;
        }
        callPeer = caller;
        pendingCallerUdpPort = callerUdpPort;
        showIncomingCallBanner(caller);
    }

    @FXML
    private void handleAcceptCall() {
        hideIncomingCallBanner();
        try {
            activeCall = new VoiceCall(InetAddress.getLocalHost(), pendingCallerUdpPort);
            int myUdpPort = activeCall.start();
            currentClient.sendCallAccept(callPeer, myUdpPort);
            showActiveCallScreen(callPeer);
            startCallTimer();
        } catch (Exception e) {
            e.printStackTrace();
            Notification.showMessageNotif("Call", "Could not start audio");
            currentClient.sendCallReject(callPeer);
            callPeer = null;
        }
        stopRingtone();
    }

    @FXML
    private void handleDeclineCall() {
        hideIncomingCallBanner();
        currentClient.sendCallReject(callPeer);
        callPeer = null;
        stopRingtone();
    }

    public void onCallAccepted(int theirUdpPort) {
        hideOutgoingCallScreen();
        try {
            activeCall = new VoiceCall(pendingLocalUdpPort, InetAddress.getLocalHost(), theirUdpPort);
            activeCall.start();
            showActiveCallScreen(callPeer);
            startCallTimer();
        } catch (Exception e) {
            e.printStackTrace();
            endCallCleanup();
            Notification.showMessageNotif("Call", "Could not start audio");
        }
    }

    public void onCallRejected() {
        hideOutgoingCallScreen();
        Notification.showMessageNotif(callPeer != null ? callPeer : "Call", "Call declined");
        callPeer = null;
    }

    public void onCallEnded() {
        String peer = callPeer;
        endCallCleanup();
        Notification.showMessageNotif(peer != null ? peer : "Call", "Call ended");
        callPeer = null;
    }

    @FXML
    private void handleMuteToggle() {
        if (activeCall == null) return;
        boolean nowMuted = !activeCall.isMuted();
        activeCall.setMuted(nowMuted);
        muteBtn.setText(nowMuted ? "Unmute" : "Mute");
        muteBtn.getStyleClass().removeAll("mute-active", "mute-inactive");
        muteBtn.getStyleClass().add(nowMuted ? "mute-active" : "mute-inactive");
    }

    @FXML
    private void handleEndCall() {
        if (callPeer != null) currentClient.sendCallEnd(callPeer);
        endCallCleanup();
    }

    private void endCallCleanup() {
        if (activeCall != null) {
            activeCall.stop();
            activeCall = null;
        }
        if (callTimer != null) {
            callTimer.stop();
            callTimer = null;
        }
        callSeconds = 0;
        Platform.runLater(() -> {
            hideActiveCallScreen();
            hideOutgoingCallScreen();
            hideIncomingCallBanner();
        });
    }

    private void startCallTimer() {
        callSeconds = 0;
        callTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            callSeconds++;
            int m = callSeconds / 60, s = callSeconds % 60;
            callDurationLabel.setText(String.format("%02d:%02d", m, s));
        }));
        callTimer.setCycleCount(Timeline.INDEFINITE);
        callTimer.play();
    }

    private void showIncomingCallBanner(String caller) {
        int callerId = RemoteApi.getUserIdByUsername(caller);
        callerPicBanner.setImage(ImageHandler.getProfileImage(callerId));
        callerNameBanner.setText(RemoteApi.getNameById(callerId)); // int ✓
        incomingCallBanner.setManaged(true);
        incomingCallBanner.setVisible(true);
        Animations.translateY(incomingCallBanner, 250, -20, 0, null).play();
        startRingtone();
    }

    private void hideIncomingCallBanner() {
        if (!incomingCallBanner.isVisible()) return;
        Animations.translateY(incomingCallBanner, 200, 0, -20, () -> {
            incomingCallBanner.setVisible(false);
            incomingCallBanner.setManaged(false);
            Platform.runLater(() -> {
                messageContainer.layout();
                scrollPane.layout();
                Platform.runLater(() -> scrollPane.setVvalue(1.0));
            });
        }).play();
    }

    private void showOutgoingCallScreen(String peer) {
        int peerId = RemoteApi.getUserIdByUsername(peer);
        callScreenPic.setImage(ImageHandler.getProfileImage(peerId));
        callScreenName.setText(RemoteApi.getNameById(peerId)); // int ✓
        callDurationLabel.setText("Calling…");
        callScreen.setManaged(true);
        callScreen.setVisible(true);
        Animations.fadeIn(callScreen);
    }

    private void hideOutgoingCallScreen() {
        if (activeCall == null) {
            callScreen.setVisible(false);
            callScreen.setManaged(false);
        }
    }

    private void showActiveCallScreen(String peer) {
        int peerId = RemoteApi.getUserIdByUsername(peer);
        callScreenPic.setImage(ImageHandler.getProfileImage(peerId));
        callScreenName.setText(RemoteApi.getNameById(peerId)); // was getNameById(peer) — String bug fixed ✓
        muteBtn.setText("Mute");
        muteBtn.getStyleClass().removeAll("mute-active", "mute-inactive");
        muteBtn.getStyleClass().add("mute-inactive");
        callScreen.setManaged(true);
        callScreen.setVisible(true);
        Animations.fadeIn(callScreen);
    }

    private void hideActiveCallScreen() {
        if (!callScreen.isVisible()) return;
        Animations.fade(callScreen, 200, 1, 0, () -> {
            callScreen.setVisible(false);
            callScreen.setManaged(false);
        }).play();
    }

    private void startRingtone() {
        try {
            var url = getClass().getResource("/com/example/drafts/sounds/ringtone.wav");
            if (url == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            ringtoneClip = AudioSystem.getClip();
            ringtoneClip.open(ais);
            ringtoneClip.loop(Clip.LOOP_CONTINUOUSLY);
            ringtoneClip.start();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void stopRingtone() {
        if (ringtoneClip != null) {
            ringtoneClip.stop();
            ringtoneClip.close();
            ringtoneClip = null;
        }
    }
}

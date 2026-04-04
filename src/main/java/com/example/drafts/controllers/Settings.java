package com.example.drafts.controllers;

import com.example.drafts.Controller;
import com.example.drafts.SceneManager;
import com.example.drafts.utils.Animations;
import com.example.drafts.utils.ChatBackground;
import com.example.drafts.utils.Notification;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.image.*;

import java.util.prefs.Preferences;

public class Settings {
    private static final String KEY_NOTIFICATIONS    = "notificationsEnabled";
    private static final String KEY_CHAT_NOTIF       = "chatNotificationsEnabled";
    private static final String KEY_NOTIF_SOUND      = "notificationSound";
    private static final String KEY_ENTER_TO_SEND    = "enterToSend";
    private static final String KEY_FONT_SIZE        = "fontSize";
    private static final String KEY_MESSAGE_SPACING  = "messageSpacing";
    private static final String KEY_AUTO_SCROLL      = "autoScroll";
    private static final String KEY_CHAT_BACKGROUND  = "chatBackground";

    @FXML private CheckBox notificationsEnabled;
    @FXML private CheckBox chatNotificationsEnabled;
    @FXML private CheckBox notificationSound;
    @FXML private CheckBox enterToSend;
    @FXML private CheckBox autoScroll;
    @FXML private TextField fontSize;
    @FXML private TextField messageSpacing;
    @FXML private HBox backgroundPicker;

    @FXML private VBox root;

    private final Preferences prefs = Preferences.userNodeForPackage(Settings.class);
    private ChatBackground selectedBackground;

    @FXML
    public void initialize() {
        buildBackgroundPicker();
        loadSettings();
        Animations.fadeIn(root);
    }

    private void buildBackgroundPicker() {
        for (ChatBackground bg : ChatBackground.values()) {
            VBox tile = new VBox(6);
            tile.setAlignment(Pos.CENTER);
            tile.getStyleClass().add("bg-tile");
            tile.setPrefSize(100, 100);

            if (bg.file != null) {
                try {
                    ImageView preview = new ImageView(
                            new Image(SceneManager.class.getResourceAsStream("Pictures/" + bg.file))
                    );
                    preview.setFitWidth(85);
                    preview.setFitHeight(65);
                    preview.setPreserveRatio(false);
                    tile.getChildren().add(preview);
                } catch (Exception e) {
                    tile.getChildren().add(new Label("?"));
                }
            } else {
                Pane empty = new Pane();
                empty.setPrefSize(85, 65);
                empty.getStyleClass().add("bg-tile-empty");
                tile.getChildren().add(empty);
            }

            tile.getChildren().add(new Label(bg.label));
            tile.setOnMouseClicked(e -> selectBackground(bg, tile));
            backgroundPicker.getChildren().add(tile);
        }
    }

    private void selectBackground(ChatBackground bg, VBox tile) {
        selectedBackground = bg;
        backgroundPicker.getChildren().forEach(n -> n.getStyleClass().remove("bg-tile-selected"));
        tile.getStyleClass().add("bg-tile-selected");
    }

    private void loadSettings() {
        notificationsEnabled.setSelected(prefs.getBoolean(KEY_NOTIFICATIONS, true));
        chatNotificationsEnabled.setSelected(prefs.getBoolean(KEY_CHAT_NOTIF, true));
        notificationSound.setSelected(prefs.getBoolean(KEY_NOTIF_SOUND, true));
        enterToSend.setSelected(prefs.getBoolean(KEY_ENTER_TO_SEND, false));
        autoScroll.setSelected(prefs.getBoolean(KEY_AUTO_SCROLL, true));
        fontSize.setText(prefs.get(KEY_FONT_SIZE, "15px"));
        messageSpacing.setText(prefs.get(KEY_MESSAGE_SPACING, "5px"));

        selectedBackground = ChatBackground.fromKey(prefs.get(KEY_CHAT_BACKGROUND, "NONE"));
        for (int i = 0; i < ChatBackground.values().length; i++) {
            if (ChatBackground.values()[i] == selectedBackground)
                backgroundPicker.getChildren().get(i).getStyleClass().add("bg-tile-selected");
        }
    }

    @FXML
    private void saveSettings() {
        prefs.putBoolean(KEY_NOTIFICATIONS,   notificationsEnabled.isSelected());
        prefs.putBoolean(KEY_CHAT_NOTIF,      chatNotificationsEnabled.isSelected());
        prefs.putBoolean(KEY_NOTIF_SOUND,     notificationSound.isSelected());
        prefs.putBoolean(KEY_ENTER_TO_SEND,   enterToSend.isSelected());
        prefs.putBoolean(KEY_AUTO_SCROLL,     autoScroll.isSelected());
        prefs.put(KEY_FONT_SIZE,              fontSize.getText().trim());
        prefs.put(KEY_MESSAGE_SPACING,        messageSpacing.getText().trim());
        prefs.put(KEY_CHAT_BACKGROUND,        selectedBackground.name());

        Controller.applySettingsLive();
        Notification.show("Settings saved!", Notification.Type.SUCCESS);
    }

    public static String get(String key, String fallback) {
        return Preferences.userNodeForPackage(Settings.class).get(key, fallback);
    }

    public static boolean getBool(String key, boolean fallback) {
        return Preferences.userNodeForPackage(Settings.class).getBoolean(key, fallback);
    }
}
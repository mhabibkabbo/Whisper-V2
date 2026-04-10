package com.example.drafts.utils;

import com.example.drafts.SceneManager;
import com.example.drafts.controllers.Settings;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;

public class Notification {

    public enum Type { INFO, SUCCESS, ERROR, MESSAGE }

    private static AudioClip notifSound;

    static {
        try {
            notifSound = new AudioClip(
                    SceneManager.class.getResource("sounds/notification.mp3").toExternalForm()
            );
        } catch (Exception e) {
            notifSound = null;
        }
    }

    public static void show(String message, Type type) {
        if (!Settings.getBool("notificationsEnabled", true)) return;

        showCard(message, type);
    }

    public static void showStrict(String message, Type type) {
        showCard(message, type);
    }

    public static void showMessageNotif(String sender, String preview) {
        if (!Settings.getBool("chatNotificationsEnabled", true)) return;

        showCard(sender + ": " + preview, Type.MESSAGE);
        playSound();
    }

    public static void playSound() {
        if (!Settings.getBool("notificationSound", true)) return;
        if (notifSound != null) notifSound.play();
    }

    private static void showCard(String message, Type type) {
        VBox container = SceneManager.getNotifContainer();
        HBox card = buildCard(message, type, container);

        VBox wrapper = new VBox(card);
        wrapper.setFillWidth(true);
        container.getChildren().add(wrapper);

        card.layoutBoundsProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> obs, Bounds oldVal, Bounds newVal) {
                if (newVal.getHeight() > 0) {
                    obs.removeListener(this);
                    double h = newVal.getHeight();
                    double w = newVal.getWidth();
                    wrapper.setMinHeight(h);
                    wrapper.setPrefHeight(h);
                    wrapper.setMaxHeight(h);

                    Animations.sequence(
                            Animations.parallel(
                                    Animations.translateX(card, 300, w + 50, 0, null),
                                    Animations.fade(card, 300, 0, 1, null)
                            ),
                            Animations.pause(3000, null),
                            Animations.parallel(
                                    Animations.translateX(card, 300, 0, w + 50, null),
                                    Animations.fade(card, 300, 1, 0, null)
                            ),
                            Animations.shrinkHeight(wrapper, 50,
                                    () -> container.getChildren().remove(wrapper))
                    ).play();
                }
            }
        });
    }

    private static HBox buildCard(String message, Type type, VBox wrapper) {
        double notifWidth = wrapper.getWidth();
        Label label = new Label(message);
        label.setWrapText(true);
        label.setMaxWidth(notifWidth);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-family: \"Outfit SemiBold\";");

        HBox card = new HBox(label);
        card.setPadding(new Insets(10));
        card.setMaxWidth(notifWidth);
        card.setOpacity(0);
        card.setStyle(
                "-fx-background-color: " + bgColor(type) + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 10, 0, 0, 2);"
        );
        return card;
    }

    private static String bgColor(Type type) {
        return switch (type) {
            case SUCCESS -> "#3d8b5e";
            case ERROR   -> "#c0392b";
            case INFO    -> "#2471a3";
            case MESSAGE -> "#5c4b8a";
        };
    }
}
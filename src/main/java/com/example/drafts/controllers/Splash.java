package com.example.drafts.controllers;

import com.example.drafts.SceneManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.io.IOException;

public class Splash {

    public StackPane rootPane;
    @FXML private StackPane logoContainer;
    @FXML private Group outlineGroup;
    @FXML private Group fillGroup;

    public void initialize() {
        Platform.runLater(this::startAnimation);
    }

    private void startAnimation() {
        logoContainer.setScaleX(0.4);
        logoContainer.setScaleY(0.4);
        fillGroup.setScaleX(1);
        fillGroup.setScaleY(1);
        double width = fillGroup.getBoundsInParent().getWidth();
        double height = fillGroup.getBoundsInParent().getHeight();

        Rectangle clipRect = new Rectangle(width + 20, 0);
        clipRect.setLayoutX(fillGroup.getBoundsInParent().getMinX());
        clipRect.setLayoutY(fillGroup.getBoundsInParent().getMinY() - 5);
        fillGroup.setClip(clipRect);

        outlineGroup.setOpacity(0);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), outlineGroup);
        fadeIn.setToValue(1.0);

        Timeline fillAnim = new Timeline(
            new KeyFrame(Duration.ZERO, new KeyValue(clipRect.heightProperty(), 0)),
            new KeyFrame(Duration.seconds(0.65), new KeyValue(clipRect.heightProperty(), height, Interpolator.EASE_BOTH))
        );

        FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.25), logoContainer);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(0.5));

        SequentialTransition sequence = new SequentialTransition(fadeIn, fillAnim, fadeOut);
        sequence.setOnFinished(event -> loadMainScreen());
        sequence.play();
    }

    private void loadMainScreen() {
        try {
            SceneManager.switchTo("login_signup.fxml", "login_signup.css");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
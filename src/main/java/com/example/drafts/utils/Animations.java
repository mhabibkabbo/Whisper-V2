package com.example.drafts.utils;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.util.Duration;

public class Animations {
    public static final double DURATION = 500;
    public static final double DURATION_SHORT = 350;
    public static final double DURATION_LONG = 750;

    // Basics
    public static FadeTransition fade(Node node, double duration, double from, double to, Runnable onFinished) {
        FadeTransition anim = new FadeTransition(Duration.millis(duration), node);
        anim.setFromValue(from);
        anim.setToValue(to);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static TranslateTransition translateX(Node node, double duration, double from, double to, Runnable onFinished) {
        TranslateTransition anim = new TranslateTransition(Duration.millis(duration), node);
        anim.setFromX(from);
        anim.setToX(to);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static TranslateTransition translateY(Node node, double duration, double from, double to, Runnable onFinished) {
        TranslateTransition anim = new TranslateTransition(Duration.millis(duration), node);
        anim.setFromY(from);
        anim.setToY(to);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static ScaleTransition scale(Node node, double duration, Double toX, Double toY, Runnable onFinished) {
        ScaleTransition anim = new ScaleTransition(Duration.millis(duration), node);
        if (toX != null) anim.setToX(toX);
        if (toY != null) anim.setToY(toY);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static RotateTransition rotate(Node node, double duration, double toAngle, Double fromAngle, Runnable onFinished) {
        RotateTransition anim = new RotateTransition(Duration.millis(duration), node);
        anim.setToAngle(toAngle);
        if (fromAngle != null) anim.setFromAngle(fromAngle);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static PauseTransition pause(double millis, Runnable onFinished) {
        PauseTransition anim = new PauseTransition(Duration.millis(millis));
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    public static SequentialTransition sequence(Animation... transitions) {
        return new SequentialTransition(transitions);
    }

    public static ParallelTransition parallel(Animation... transitions) {
        return new ParallelTransition(transitions);
    }

    // Extra
    public static Timeline shrinkHeight(Region node, double duration, Runnable onFinished) {
        double startHeight = node.getHeight();
        node.setMinHeight(startHeight);
        node.setPrefHeight(startHeight);
        node.setMaxHeight(startHeight);

        Timeline anim = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(node.minHeightProperty(), startHeight),
                        new KeyValue(node.prefHeightProperty(), startHeight),
                        new KeyValue(node.maxHeightProperty(), startHeight)
                ),
                new KeyFrame(Duration.millis(duration),
                        new KeyValue(node.minHeightProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.prefHeightProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(node.maxHeightProperty(), 0, Interpolator.EASE_OUT)
                )
        );
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        return anim;
    }

    // Offset In / Out
    public static void offsetIn(Node node, double width, int direction, Runnable onFinished, double duration) {
        node.setVisible(true);
        TranslateTransition anim = new TranslateTransition(Duration.millis(duration), node);
        anim.setFromX(width * direction);
        anim.setToX(0);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        anim.play();
    }

    public static void offsetIn(Node node, double width, int direction, Runnable onFinished) {
        offsetIn(node, width, direction, onFinished, DURATION_SHORT);
    }

    public static void offsetIn(Node node, double width, int direction) {
        offsetIn(node, width, direction, null, DURATION_SHORT);
    }

    public static void offsetOut(Node node, double width, int direction, Runnable onFinished, double duration) {
        TranslateTransition anim = new TranslateTransition(Duration.millis(duration), node);
        anim.setFromX(0);
        anim.setToX(width * direction);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        anim.play();
    }

    public static void offsetOut(Node node, double width, int direction, Runnable onFinished) {
        offsetOut(node, width, direction, onFinished, DURATION_SHORT);
    }

    public static void offsetOut(Node node, double width, int direction) {
        offsetOut(node, width, direction, null, DURATION_SHORT);
    }

    // Fade In / Out
    public static void fadeIn(Node node, double from, double to, Runnable onFinished, double duration) {
        FadeTransition anim = new FadeTransition(Duration.millis(duration), node);
        anim.setFromValue(from);
        anim.setToValue(to);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        anim.play();
    }

    public static void fadeIn(Node node, Runnable onFinished) {
        fadeIn(node, 0, 1, onFinished, DURATION);
    }

    public static void fadeIn(Node node) {
        fadeIn(node, 0, 1, null, DURATION);
    }

    public static void fadeOut(Node node, double from, double to, Runnable onFinished, double duration) {
        FadeTransition anim = new FadeTransition(Duration.millis(duration), node);
        anim.setFromValue(from);
        anim.setToValue(to);
        if (onFinished != null) anim.setOnFinished(e -> onFinished.run());
        anim.play();
    }

    public static void fadeOut(Node node, Runnable onFinished) {
        fadeOut(node, 1, 0, onFinished, DURATION);
    }

    public static void fadeOut(Node node) {
        fadeOut(node, 1, 0, null, DURATION);
    }
}

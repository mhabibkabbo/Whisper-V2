package com.example.drafts.controllers;

import com.example.drafts.SceneManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class AppView {
    @FXML private StackPane content;
    private static AppView instance;

    @FXML
    public void initialize() {
        instance = this;
        switchView("message_view.fxml");
    }

    public static void switchView(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneManager.class.getResource("fxml/" + fxml)
            );
            Node view = loader.load();
            instance.content.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

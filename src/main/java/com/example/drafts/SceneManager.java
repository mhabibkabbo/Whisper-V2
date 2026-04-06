package com.example.drafts;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneManager {
    private static Stage stage;
    private static StackPane rootWrapper;
    private static final String fxmlFolder = "fxml/";
    private static final String cssFolder = "css/";
    private static VBox notifContainer;

    public static Stage getStage() { return stage; }

    public static StackPane getRootWrapper() { return rootWrapper; }

    public static void init(Stage primaryStage, String initialFxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFolder + initialFxml));
        Parent content = loader.load();

        rootWrapper = new StackPane(content);
        Scene scene = new Scene(rootWrapper, 1000, 700);
        scene.getStylesheets().add(SceneManager.class.getResource("css/global.css").toExternalForm());

        stage = primaryStage;
        stage.setScene(scene);
        stage.setTitle("Whisper");
        stage.getIcons().add(new Image(SceneManager.class.getResourceAsStream("icons/logo.png")));
        stage.show();

        initNotifContainer(rootWrapper);
    }

    public static void initNotifContainer(StackPane wrapper) {
        notifContainer = new VBox(10); // 8px gap between notifications
        notifContainer.setPickOnBounds(false);
        notifContainer.setPadding(new Insets(10, 10, 0, 0));
        notifContainer.setMaxSize(350, Region.USE_PREF_SIZE);
        StackPane.setAlignment(notifContainer, Pos.TOP_RIGHT);
        wrapper.getChildren().add(notifContainer);
    }

    public static VBox getNotifContainer() {
        return notifContainer;
    }

    public static void switchTo(String fxml, String css) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFolder + fxml));
        Parent content = loader.load();

        Scene scene = stage.getScene();
        scene.getStylesheets().removeIf(s -> !s.contains("global"));
        if (css != null) {
            scene.getStylesheets().add(SceneManager.class.getResource(cssFolder + css).toExternalForm());
        }

        // Replace only index 0 (scene content), preserve notification layer above
        rootWrapper.getChildren().set(0, content);
    }

    public static void switchTo(String fxml) throws IOException {
        switchTo(fxml, null);
    }

    public static <T> T switchToAndGet(String fxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlFolder + fxml));
        Parent content = loader.load();
        rootWrapper.getChildren().set(0, content);
        return loader.getController();
    }
}
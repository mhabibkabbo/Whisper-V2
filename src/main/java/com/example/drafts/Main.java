package com.example.drafts;

import javafx.application.Application;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        Font.loadFont(getClass().getResourceAsStream("fonts/Outfit-Regular.ttf"), 10);
        Font.loadFont(getClass().getResourceAsStream("fonts/Outfit-Bold.ttf"), 10);
        Font.loadFont(getClass().getResourceAsStream("fonts/Outfit-SemiBold.ttf"), 10);

        SceneManager.init(stage, "splash.fxml");
//        SceneManager.init(stage, "login_signup.fxml");
    }
}
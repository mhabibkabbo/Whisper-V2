package com.example.drafts;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("login.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setScene(scene);
        stage.setTitle("Whisper");
        stage.setResizable(false);
        stage.setWidth(875);
        stage.setHeight(650);
        stage.show();
    }

    public static void main(String[] args) throws IOException {
       // Socket socket = new Socket("localhost", 1234);
        launch();
    }
}
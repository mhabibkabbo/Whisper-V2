package com.example.drafts;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String username;
    private Controller controller;
    public Client(String username, Controller controller) {
        try {
            this.socket = new Socket("localhost", 5000); // server IP
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = username;
            this.controller = controller;
            writer.write(username);
            writer.newLine();
            writer.flush();

            Thread thread = new Thread(this::listenForMessages);
            thread.setDaemon(true);
            thread.start();

        } catch (IOException e) {
            closeEverything();
        }
    }

    public void sendMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeEverything();
        }
    }

    private void listenForMessages() {
        String message;

        try {
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("GROUP|")) {

                    String[] parts = message.split("\\|", 3);
                    int groupID = Integer.parseInt(parts[1]);
                    String sender = parts[2];
                    String msg = parts[2];

                    Platform.runLater(() ->
                            controller.receiveGroupMessage(groupID, sender, msg)
                    );

                } else if (message.startsWith("PM|")) {

                    String[] parts = message.split("\\|", 3);
                    String sender = parts[1];
                    String msg = parts[2];

                    Platform.runLater(() ->
                            controller.receivePrivateMessage(sender, msg)
                    );
                } else if (message.startsWith("GROUP_MSG|")) {
                    String[] parts = message.split("\\|", 4);
                    int groupId = Integer.parseInt(parts[1]);
                    String sender = parts[2];
                    String msg = parts[3];
                    Platform.runLater(() -> controller.receiveGroupMessage(groupId, sender, msg));
                }
            }

        } catch (IOException e) {
            closeEverything();
        }
    }

    public String getUsername(){ return username; }
    public void sendGroupMessage(int groupId, String message) throws IOException {
        writer.write("GROUP_MSG|" + groupId + "|" + message);
        writer.newLine();
        writer.flush();
    }

    public void sendPrivateMessage(String receiver, String message) throws IOException {
        writer.write("PM|" + receiver + "|" + message);
        writer.newLine();
        writer.flush();
    }
    private void closeEverything() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}

package com.example.drafts.Server.src;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            this.clientUsername = reader.readLine();
            if (clientUsername == null || clientUsername.isBlank()) {
                closeEverything();
                return;
            }
            Server.onlineUsers.put(clientUsername, this);
            System.out.println(clientUsername + " joined the chat.");
            //Server.broadcastMessage(clientUsername + " joined the chat!", this);
        } catch (IOException e) {
            closeEverything();
        }
    }

    @Override
    public void run() {
        String message;
        try {
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("GROUP|")) {

                    String groupMessage = message.substring(6);
                    Server.broadcastMessage(
                            "GROUP|" + clientUsername + "|" + groupMessage,
                            this
                    );

                } else if (message.startsWith("PM|")) {

                    String[] parts = message.split("\\|", 3);
                    String receiver = parts[1];
                    String privateMsg = parts[2];

                    Server.sendPrivateMessage(
                            clientUsername,
                            receiver,
                            privateMsg
                    );

                } else if (message.startsWith("PM_FILE|")) {
                    String[] parts = message.split("\\|", 5);
                    Server.sendRawToUser(
                            parts[1],
                            "PM_FILE|" + clientUsername + "|" + parts[2] + "|" + parts[3] + "|" + parts[4]
                    );

                } else if (message.startsWith("GROUP_FILE|")) {
                    String[] parts = message.split("\\|", 5);
                    Server.broadcastMessage(
                            "GROUP_FILE|" + parts[1] + "|" + clientUsername + "|" + parts[2] + "|" + parts[3] + "|" + parts[4],
                            this
                    );

                } else if (message.startsWith("CALL_REQUEST|")) {
                    String[] p = message.split("\\|");
                    Server.sendRawToUser(p[1], "CALL_REQUEST|" + clientUsername + "|" + p[2]);

                } else if (message.startsWith("CALL_ACCEPT|")) {
                    String[] p = message.split("\\|");
                    Server.sendRawToUser(p[1], "CALL_ACCEPT|" + clientUsername + "|" + p[2]);

                } else if (message.startsWith("CALL_REJECT|")) {
                    Server.sendRawToUser(message.split("\\|")[1], "CALL_REJECT|" + clientUsername);

                } else if (message.startsWith("CALL_END|")) {
                    Server.sendRawToUser(message.split("\\|")[1], "CALL_END|" + clientUsername);
                }
            }
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

    private void closeEverything() {
        Server.removeClient(this);
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }

    public String getClientUsername() {
        return clientUsername;
    }
}

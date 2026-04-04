package com.example.drafts;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Base64;

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

    public void sendPrivateFile(String receiver, String filename,
                                String mimeType, byte[] data) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(data);
        writer.write("PM_FILE|" + receiver + "|" + filename + "|" + mimeType + "|" + b64);
        writer.newLine();
        writer.flush();
    }

    public void sendGroupFile(int groupId, String filename,
                              String mimeType, byte[] data) throws IOException {
        String b64 = Base64.getEncoder().encodeToString(data);
        writer.write("GROUP_FILE|" + groupId + "|" + filename + "|" + mimeType + "|" + b64);
        writer.newLine();
        writer.flush();
    }

    public void sendCallRequest(String receiver, int myUdpPort) {
        sendMessage("CALL_REQUEST|" + receiver + "|" + myUdpPort);
    }

    public void sendCallAccept(String caller, int myUdpPort) {
        sendMessage("CALL_ACCEPT|" + caller + "|" + myUdpPort);
    }

    public void sendCallReject(String caller) {
        sendMessage("CALL_REJECT|" + caller);
    }

    public void sendCallEnd(String peer) {
        sendMessage("CALL_END|" + peer);
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
                } else if (message.startsWith("PM_FILE|")) {
                    String[] parts = message.split("\\|", 5);
                    String sender   = parts[1];
                    String filename = parts[2];
                    String mimeType = parts[3];
                    byte[] data     = Base64.getDecoder().decode(parts[4]);
                    Platform.runLater(() -> controller.receivePrivateFile(sender, filename, mimeType, data));

                } else if (message.startsWith("GROUP_FILE|")) {
                    String[] parts  = message.split("\\|", 6);
                    int groupId     = Integer.parseInt(parts[1]);
                    String sender   = parts[2];
                    String filename = parts[3];
                    String mimeType = parts[4];
                    byte[] data     = Base64.getDecoder().decode(parts[5]);
                    Platform.runLater(() -> controller.receiveGroupFile(groupId, sender, filename, mimeType, data));

                } else if (message.startsWith("CALL_REQUEST|")) {
                    String[] p = message.split("\\|");
                    String caller      = p[1];
                    int callerUdpPort  = Integer.parseInt(p[2]);
                    Platform.runLater(() -> controller.onIncomingCall(caller, callerUdpPort));

                } else if (message.startsWith("CALL_ACCEPT|")) {
                    String[] p = message.split("\\|");
                    int theirUdpPort = Integer.parseInt(p[2]);
                    Platform.runLater(() -> controller.onCallAccepted(theirUdpPort));

                } else if (message.startsWith("CALL_REJECT|")) {
                    Platform.runLater(() -> controller.onCallRejected());

                } else if (message.startsWith("CALL_END|")) {
                    Platform.runLater(() -> controller.onCallEnded());
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

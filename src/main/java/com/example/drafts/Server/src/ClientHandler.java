package com.example.drafts.Server.src;

import com.example.drafts.Database;
import com.example.drafts.Conversation;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientUsername;
    private boolean rpcMode;

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
            if ("__RPC__".equals(clientUsername)) {
                rpcMode = true;
            } else {
                Server.onlineUsers.put(clientUsername, this);
                System.out.println(clientUsername + " joined the chat.");
            }
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
                if (rpcMode) {
                    handleRpc(message);
                    continue;
                }
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

                    int senderId = Database.getUserIdByUsername(clientUsername);
                    int receiverId = Database.getUserIdByUsername(receiver);
                    if (senderId != -1 && receiverId != -1) {
                        Database.saveMessage(senderId, receiverId, privateMsg);
                        Database.upsertConversation(clientUsername, receiver, privateMsg);
                    }

                    Server.sendPrivateMessage(
                            clientUsername,
                            receiver,
                            privateMsg
                    );

                } else if (message.startsWith("PM_FILE|")) {
                    String[] parts = message.split("\\|", 5);
                    String receiver = parts[1];
                    String filename = parts[2];
                    String mimeType = parts[3];
                    byte[] data = Base64.getDecoder().decode(parts[4]);

                    int senderId = Database.getUserIdByUsername(clientUsername);
                    int receiverId = Database.getUserIdByUsername(receiver);
                    if (senderId != -1 && receiverId != -1) {
                        Database.saveMessage(senderId, receiverId, "", data, filename, mimeType);
                        Database.upsertConversation(clientUsername, receiver, "📎 " + filename);
                    }

                    Server.sendRawToUser(
                            receiver,
                            "PM_FILE|" + clientUsername + "|" + filename + "|" + mimeType + "|" + parts[4]
                    );

                } else if (message.startsWith("GROUP_FILE|")) {
                    String[] parts = message.split("\\|", 5);
                    int groupId = Integer.parseInt(parts[1]);
                    String filename = parts[2];
                    String mimeType = parts[3];
                    byte[] data = Base64.getDecoder().decode(parts[4]);
                    Database.saveGroupMessage(groupId, clientUsername, "", data, filename, mimeType);
                    Server.broadcastMessage(
                            "GROUP_FILE|" + groupId + "|" + clientUsername + "|" + filename + "|" + mimeType + "|" + parts[4],
                            this
                    );

                } else if (message.startsWith("GROUP_MSG|")) {
                    String[] parts = message.split("\\|", 3);
                    int groupId = Integer.parseInt(parts[1]);
                    String groupMessage = parts[2];
                    Database.saveGroupMessage(groupId, clientUsername, groupMessage);
                    Server.broadcastMessage(
                            "GROUP_MSG|" + groupId + "|" + clientUsername + "|" + groupMessage,
                            this
                    );
                } else if (message.startsWith("LOAD_PM|")) {
                    String[] parts = message.split("\\|", 2);
                    if (parts.length == 2 && !parts[1].isBlank()) {
                        Server.sendPmHistory(clientUsername, parts[1]);
                    }
                } else if (message.startsWith("LOAD_GROUP|")) {
                    String[] parts = message.split("\\|", 2);
                    if (parts.length == 2 && !parts[1].isBlank()) {
                        int groupId = Integer.parseInt(parts[1]);
                        Server.sendGroupHistory(clientUsername, groupId);
                    }

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

    private void handleRpc(String message) {
        try {
            if (message.startsWith("RPC_LOGIN|")) {
                String[] p = message.split("\\|", 3);
                String username = ub64(p[1]);
                String password = ub64(p[2]);
                int userId = Database.login(username, password);
                if (userId != -1) {
                    sendLine("OK|" + userId);
                } else {
                    sendLine("FAIL");
                }
            } else if (message.startsWith("RPC_SIGNUP|")) {
                String[] p = message.split("\\|", 5);
                String name = ub64(p[1]);
                String username = ub64(p[2]);
                String passwordHash = ub64(p[3]);
                byte[] pic = p[4].isEmpty() ? null : Base64.getDecoder().decode(p[4]);
                boolean ok = Database.insertUser(name, username, passwordHash, pic);
                sendLine(ok ? "OK" : "FAIL");
            } else if (message.startsWith("RPC_SEARCH_USERS|")) {
                String[] p = message.split("\\|", 2);
                List<String> users = Database.searchUsers(ub64(p[1]));
                for (String user : users) sendLine("USER|" + b64(user));
            } else if (message.startsWith("RPC_GET_USER_ID|")) {
                String[] p = message.split("\\|", 2);
                sendLine("OK|" + Database.getUserIdByUsername(ub64(p[1])));
            } else if (message.startsWith("RPC_GET_USERNAME|")) {
                String[] p = message.split("\\|", 2);
                sendLine("OK|" + b64(Database.getUsernameById(Integer.parseInt(p[1]))));
            } else if (message.startsWith("RPC_GET_NAME|")) {
                String[] p = message.split("\\|", 2);
                sendLine("OK|" + b64(Database.getNameById(Integer.parseInt(p[1]))));
            } else if (message.startsWith("RPC_GET_PROFILE_PIC|")) {
                String[] p = message.split("\\|", 2);
                byte[] pic = Database.getProfilePicture(Integer.parseInt(p[1]));
                sendLine("OK|" + b64bytes(pic));
            } else if (message.startsWith("RPC_GET_CONVERSATIONS|")) {
                String[] p = message.split("\\|", 2);
                List<Conversation> list = Database.getConversations(ub64(p[1]));
                for (Conversation c : list) {
                    sendLine("CONV|" + c.getId() + "|" + b64(c.getUser1()) + "|" + b64(c.getUser2()) + "|" +
                            b64(c.getLastMessage()) + "|" + b64(c.getLastTimestamp()));
                }
            } else if (message.startsWith("RPC_GET_GROUPS_FOR_USER|")) {
                String[] p = message.split("\\|", 2);
                List<String[]> groups = Database.getGroupsForUser(ub64(p[1]));
                for (String[] g : groups) {
                    sendLine("GROUP|" + g[0] + "|" + b64(g[1]));
                }
            } else if (message.startsWith("RPC_GET_LAST_GROUP_MESSAGE|")) {
                String[] p = message.split("\\|", 2);
                String[] last = Database.getLastGroupMessage(Integer.parseInt(p[1]));
                if (last == null) sendLine("NONE");
                else sendLine("LAST|" + b64(last[0]) + "|" + b64(last[1]) + "|" + b64(last[2]));
            } else if (message.startsWith("RPC_CREATE_GROUP|")) {
                String[] p = message.split("\\|", 4);
                String groupName = ub64(p[1]);
                String createdBy = ub64(p[2]);
                String membersCsv = ub64(p[3]);
                List<String> members = new ArrayList<>();
                if (!membersCsv.isBlank()) {
                    for (String m : membersCsv.split(",")) {
                        if (!m.isBlank()) members.add(m);
                    }
                }
                int id = Database.createGroup(groupName, createdBy, members);
                sendLine("OK|" + id);
            } else if (message.startsWith("RPC_UPDATE_NAME|")) {
                String[] p = message.split("\\|", 3);
                boolean ok = Database.updateName(Integer.parseInt(p[1]), ub64(p[2]));
                sendLine(ok ? "OK" : "FAIL");
            } else if (message.startsWith("RPC_UPDATE_PASSWORD|")) {
                String[] p = message.split("\\|", 4);
                boolean ok = Database.updatePassword(Integer.parseInt(p[1]), ub64(p[2]), ub64(p[3]));
                sendLine(ok ? "OK" : "FAIL");
            } else if (message.startsWith("RPC_UPDATE_PROFILE_PIC|")) {
                String[] p = message.split("\\|", 3);
                byte[] pic = p[2].isEmpty() ? null : Base64.getDecoder().decode(p[2]);
                boolean ok = Database.updateProfilePicture(Integer.parseInt(p[1]), pic);
                sendLine(ok ? "OK" : "FAIL");
            } else {
                sendLine("FAIL");
            }
            sendLine("__END__");
        } catch (Exception e) {
            try {
                sendLine("FAIL");
                sendLine("__END__");
            } catch (IOException ignored) {}
        }
    }

    private void sendLine(String line) throws IOException {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private static String ub64(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String b64(String value) {
        String safe = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String b64bytes(byte[] value) {
        if (value == null || value.length == 0) return "";
        return Base64.getEncoder().encodeToString(value);
    }
}

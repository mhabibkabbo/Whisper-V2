package com.example.drafts.Server.src;

import com.example.drafts.Database;
import com.example.drafts.GroupMessage;
import com.example.drafts.Message;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server {

    public static CopyOnWriteArrayList<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    public static ConcurrentHashMap<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();
    public static void main(String[] args) {
        Database.createUserTable();
        Database.createMessageTable();
        Database.createConversationTable();
        Database.createGroupTables();
        Database.migrateAttachmentColumns();

        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server started on port 5000...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void sendRawToUser(String username, String message) {
        ClientHandler handler = onlineUsers.get(username);
        if (handler != null) handler.sendMessage(message);
    }

    // Broadcast message to all clients
    public static void broadcastMessage(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        onlineUsers.remove(clientHandler.getClientUsername());
        System.out.println("Client disconnected: " + clientHandler.getClientUsername());
    }

    public static void sendPrivateMessage(String sender,
                                          String receiver,
                                          String message) {

        ClientHandler target = onlineUsers.get(receiver);

        if (target != null) {
            target.sendMessage("PM|" + sender + "|" + message);
        }
    }

    public static void sendPmHistory(String requester, String peer) {
        ClientHandler target = onlineUsers.get(requester);
        if (target == null) return;

        int requesterId = Database.getUserIdByUsername(requester);
        int peerId = Database.getUserIdByUsername(peer);
        if (requesterId == -1 || peerId == -1) {
            target.sendMessage("PM_HISTORY_END|" + peer);
            return;
        }

        List<Message> history = Database.getMessagesBetween(requesterId, peerId);
        for (Message msg : history) {
            String senderUsername = Database.getUsernameById(msg.getSenderId());
            target.sendMessage(
                    "PM_HISTORY_ITEM|" + peer + "|" +
                            enc(senderUsername) + "|" +
                            enc(msg.getTimestamp()) + "|" +
                            enc(msg.getContent()) + "|" +
                            enc(msg.getAttachmentName()) + "|" +
                            enc(msg.getAttachmentType()) + "|" +
                            encBytes(msg.getAttachmentData())
            );
        }
        target.sendMessage("PM_HISTORY_END|" + peer);
    }

    public static void sendGroupHistory(String requester, int groupId) {
        ClientHandler target = onlineUsers.get(requester);
        if (target == null) return;

        List<GroupMessage> history = Database.getGroupMessages(groupId);
        for (GroupMessage msg : history) {
            target.sendMessage(
                    "GROUP_HISTORY_ITEM|" + groupId + "|" +
                            enc(msg.getSenderUsername()) + "|" +
                            enc(msg.getTimestamp()) + "|" +
                            enc(msg.getContent()) + "|" +
                            enc(msg.getAttachmentName()) + "|" +
                            enc(msg.getAttachmentType()) + "|" +
                            encBytes(msg.getAttachmentData())
            );
        }
        target.sendMessage("GROUP_HISTORY_END|" + groupId);
    }

    private static String enc(String value) {
        String safe = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safe.getBytes());
    }

    private static String encBytes(byte[] value) {
        if (value == null || value.length == 0) return "";
        return Base64.getEncoder().encodeToString(value);
    }
}

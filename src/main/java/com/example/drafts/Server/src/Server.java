package com.example.drafts.Server.src;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class Server {

    public static CopyOnWriteArrayList<ClientHandler> clients =
            new CopyOnWriteArrayList<>();

    public static ConcurrentHashMap<String, ClientHandler> onlineUsers =
            new ConcurrentHashMap<>();
    public static void main(String[] args) {
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
}

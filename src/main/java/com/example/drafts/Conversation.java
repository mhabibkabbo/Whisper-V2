package com.example.drafts;

public class Conversation {

    private int id;
    private String user1;
    private String user2;
    private String lastMessage;

    public Conversation(int id, String user1, String user2, String lastMessage) {
        this.id = id;
        this.user1 = user1;
        this.user2 = user2;
        this.lastMessage = lastMessage;
    }

    public String getOtherUser(String myUsername) {
        return myUsername.equals(user1) ? user2 : user1;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public int getId() {
        return id;
    }
}

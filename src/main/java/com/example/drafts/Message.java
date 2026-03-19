package com.example.drafts;

public class Message {
    private int senderId;
    private String content;
    private String timestamp;

    public Message(int senderId, String content, String timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public int getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
}
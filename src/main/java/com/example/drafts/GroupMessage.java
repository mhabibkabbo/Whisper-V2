package com.example.drafts;

public class GroupMessage {
    private String senderUsername;
    private String content;
    private String timestamp;
    private int groupId;

    public GroupMessage(String senderUsername, String content, String timestamp) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
    }

    public GroupMessage(int groupId, String senderUsername, String content, String timestamp) {
        this.groupId = groupId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSenderUsername() { return senderUsername; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public int getGroupId() { return groupId; }
}
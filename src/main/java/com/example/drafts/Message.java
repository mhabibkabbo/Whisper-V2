package com.example.drafts;

public class Message {
    private int senderId;
    private String content;
    private String timestamp;
    private byte[] attachmentData;
    private String attachmentName;
    private String attachmentType;

    public Message(int senderId, String content, String timestamp) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
    }

    public Message(int senderId, String content, String timestamp,
                   byte[] attachmentData, String attachmentName, String attachmentType) {
        this.senderId = senderId;
        this.content = content;
        this.timestamp = timestamp;
        this.attachmentData = attachmentData;
        this.attachmentName = attachmentName;
        this.attachmentType = attachmentType;
    }

    public int getSenderId() { return senderId; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public boolean hasAttachment() { return attachmentData != null; }
    public byte[]  getAttachmentData() { return attachmentData; }
    public String  getAttachmentName() { return attachmentName; }
    public String  getAttachmentType() { return attachmentType; }

}
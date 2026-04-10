package com.example.drafts;

public class GroupMessage {
    private String senderUsername;
    private String content;
    private String timestamp;
    private int groupId;
    private byte[] attachmentData;
    private String attachmentName;
    private String attachmentType;

    public GroupMessage(String senderUsername, String content, String timestamp,
                   byte[] attachmentData, String attachmentName, String attachmentType) {
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = timestamp;
        this.attachmentData = attachmentData;
        this.attachmentName = attachmentName;
        this.attachmentType = attachmentType;
    }

    public String getSenderUsername() { return senderUsername; }
    public String getContent() { return content; }
    public String getTimestamp() { return timestamp; }
    public int getGroupId() { return groupId; }
    public boolean hasAttachment() { return attachmentData != null; }
    public byte[]  getAttachmentData() { return attachmentData; }
    public String  getAttachmentName() { return attachmentName; }
    public String  getAttachmentType() { return attachmentType; }

}
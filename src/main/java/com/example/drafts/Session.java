package com.example.drafts;

public class Session {

    private static int currentUserId;

    public static void setCurrentUserId(int id) {
        currentUserId = id;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    private static int currentGroupId = -1;

    public static void setCurrentGroupId(int id) { currentGroupId = id; }
    public static int getCurrentGroupId() { return currentGroupId; }
    public static void clearGroup() { currentGroupId = -1; }

    public static void clearCurrentChatUser() {
        currentGroupId = -1;
    }
}

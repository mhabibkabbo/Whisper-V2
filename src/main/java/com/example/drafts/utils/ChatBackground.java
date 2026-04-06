package com.example.drafts.utils;

public enum ChatBackground {
    NONE("None", null, "chat-bg-none"),
    DEFAULT("Default", "chat-background.jpg", "chat-bg"),
    DOODLE("Doodles", "doodle-background.jpg", "chat-bg-doodle"),
    DOTS("Dots", "dots-background.jpg", "chat-bg-dots"),
    LINES("Lines", "lines-background.jpg", "chat-bg-lines");

    public final String label;
    public final String file;
    public final String cssClass;

    ChatBackground(String label, String file, String cssClass) {
        this.label = label;
        this.file = file;
        this.cssClass = cssClass;
    }

    public static ChatBackground fromKey(String key) {
        for (ChatBackground b : values())
            if (b.name().equals(key)) return b;
        return DEFAULT;
    }
}
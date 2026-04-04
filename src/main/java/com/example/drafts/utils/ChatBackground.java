package com.example.drafts.utils;

public enum ChatBackground {
    NONE("None", null, "chat-bg-none"),
    DEFAULT("Default", "chat-background.jpg", "chat-bg"),
    DOODLE("Bubbles", "bg_bubbles.png", "chat-bg-bubbles"),
    POLKA("Polka Dots", "bg_polka.png", "chat-bg-polka"),
    LINES("Lines", "bg_lines.png", "chat-bg-lines");

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
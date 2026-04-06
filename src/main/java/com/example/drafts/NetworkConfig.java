package com.example.drafts;

public final class NetworkConfig {
    private NetworkConfig() {}

    public static final String SERVER_HOST =
            System.getProperty("whisper.server.host", "localhost");
    public static final int SERVER_PORT =
            Integer.parseInt(System.getProperty("whisper.server.port", "5000"));
}

package com.example.drafts;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class RemoteApi {
    private static final String HOST = NetworkConfig.SERVER_HOST;
    private static final int PORT = NetworkConfig.SERVER_PORT;
    private static final String END = "__END__";

    private static List<String> execute(String command) {
        List<String> lines = new ArrayList<>();
        try (Socket socket = new Socket(HOST, PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            writer.write("__RPC__");
            writer.newLine();
            writer.write(command);
            writer.newLine();
            writer.flush();

            String line;
            while ((line = reader.readLine()) != null) {
                if (END.equals(line)) break;
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static String b64(String value) {
        String safe = value == null ? "" : value;
        return Base64.getEncoder().encodeToString(safe.getBytes(StandardCharsets.UTF_8));
    }

    private static String ub64(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static byte[] ub64bytes(String value) {
        if (value == null || value.isEmpty()) return null;
        return Base64.getDecoder().decode(value);
    }

    public static int login(String username, String password) {
        List<String> out = execute("RPC_LOGIN|" + b64(username) + "|" + b64(password));
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return -1;
        return Integer.parseInt(out.get(0).split("\\|", 2)[1]);
    }

    public static boolean insertUser(String name, String username, String passwordHash, byte[] profileImageBytes) {
        String pic = profileImageBytes == null ? "" : Base64.getEncoder().encodeToString(profileImageBytes);
        List<String> out = execute("RPC_SIGNUP|" + b64(name) + "|" + b64(username) + "|" + b64(passwordHash) + "|" + pic);
        return !out.isEmpty() && out.get(0).equals("OK");
    }

    public static List<String> searchUsers(String keyword) {
        List<String> out = execute("RPC_SEARCH_USERS|" + b64(keyword));
        List<String> users = new ArrayList<>();
        for (String line : out) {
            if (line.startsWith("USER|")) users.add(ub64(line.split("\\|", 2)[1]));
        }
        return users;
    }

    public static String getUsernameById(int userId) {
        List<String> out = execute("RPC_GET_USERNAME|" + userId);
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return null;
        return ub64(out.get(0).split("\\|", 2)[1]);
    }

    public static int getUserIdByUsername(String username) {
        List<String> out = execute("RPC_GET_USER_ID|" + b64(username));
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return -1;
        return Integer.parseInt(out.get(0).split("\\|", 2)[1]);
    }

    public static String getNameById(int id) {
        List<String> out = execute("RPC_GET_NAME|" + id);
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return null;
        return ub64(out.get(0).split("\\|", 2)[1]);
    }

    public static byte[] getProfilePicture(int id) {
        List<String> out = execute("RPC_GET_PROFILE_PIC|" + id);
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return null;
        return ub64bytes(out.get(0).split("\\|", 2)[1]);
    }

    public static List<Conversation> getConversations(String username) {
        List<String> out = execute("RPC_GET_CONVERSATIONS|" + b64(username));
        List<Conversation> list = new ArrayList<>();
        for (String line : out) {
            if (!line.startsWith("CONV|")) continue;
            String[] p = line.split("\\|", 6);
            list.add(new Conversation(
                    Integer.parseInt(p[1]),
                    ub64(p[2]),
                    ub64(p[3]),
                    ub64(p[4]),
                    ub64(p[5])
            ));
        }
        return list;
    }

    public static List<String[]> getGroupsForUser(String username) {
        List<String> out = execute("RPC_GET_GROUPS_FOR_USER|" + b64(username));
        List<String[]> groups = new ArrayList<>();
        for (String line : out) {
            if (!line.startsWith("GROUP|")) continue;
            String[] p = line.split("\\|", 3);
            groups.add(new String[]{p[1], ub64(p[2])});
        }
        return groups;
    }

    public static String[] getLastGroupMessage(int groupId) {
        List<String> out = execute("RPC_GET_LAST_GROUP_MESSAGE|" + groupId);
        if (out.isEmpty() || out.get(0).equals("NONE")) return null;
        String[] p = out.get(0).split("\\|", 4);
        return new String[]{ub64(p[1]), ub64(p[2]), ub64(p[3])};
    }

    public static int createGroup(String name, String createdBy, List<String> members) {
        String payload = String.join(",", members);
        List<String> out = execute("RPC_CREATE_GROUP|" + b64(name) + "|" + b64(createdBy) + "|" + b64(payload));
        if (out.isEmpty() || !out.get(0).startsWith("OK|")) return -1;
        return Integer.parseInt(out.get(0).split("\\|", 2)[1]);
    }

    public static boolean updateName(int userId, String newName) {
        List<String> out = execute("RPC_UPDATE_NAME|" + userId + "|" + b64(newName));
        return !out.isEmpty() && out.get(0).equals("OK");
    }

    public static boolean updatePassword(int userId, String currentRaw, String newRaw) {
        List<String> out = execute("RPC_UPDATE_PASSWORD|" + userId + "|" + b64(currentRaw) + "|" + b64(newRaw));
        return !out.isEmpty() && out.get(0).equals("OK");
    }

    public static boolean updateProfilePicture(int userId, byte[] imageBytes) {
        String payload = imageBytes == null ? "" : Base64.getEncoder().encodeToString(imageBytes);
        List<String> out = execute("RPC_UPDATE_PROFILE_PIC|" + userId + "|" + payload);
        return !out.isEmpty() && out.get(0).equals("OK");
    }
}

package com.example.drafts;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.example.drafts.PassHasher.hashPassword;

public class Database {

    public static Connection connect() {
        Connection conn = null;
        try {
            String url = "jdbc:sqlite:messenger.db";
            conn = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    public static void createUserTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            username TEXT NOT NULL UNIQUE,
            password TEXT NOT NULL,
            profile_pic BLOB
        );
    """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static String getNameById(int id) {

        String sql = "SELECT name FROM users WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getString("name");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static String getUsernameById(int userId) {

        String sql = "SELECT username FROM users WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("username");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null; // if not found
    }
    public static int getUserIdByUsername(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return -1;
    }


    public static void createMessageTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sender_id INTEGER,
            receiver_id INTEGER,
            message TEXT NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY(sender_id) REFERENCES users(id),
            FOREIGN KEY(receiver_id) REFERENCES users(id)
        );
    """;

        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static boolean insertUser(String name,
                                     String username,
                                     String password,
                                     byte[] profileImageBytes) {

        String sql = "INSERT INTO users(name, username, password, profile_pic) VALUES(?, ?, ?, ?)";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, username);
            pstmt.setString(3, password);

            if (profileImageBytes != null) {
                pstmt.setBytes(4, profileImageBytes);
            } else {
                pstmt.setNull(4, Types.BLOB);
            }

            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {

            if (e.getMessage().contains("UNIQUE constraint failed")) {
                System.out.println("Username already exists!");
            } else {
                e.printStackTrace();
            }

            return false;
        }
    }


    public static int login(String username, String password) {

        String sql = "SELECT id, password FROM users WHERE username = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {

                String storedHash = rs.getString("password");
                String inputHash = hashPassword(password);

                if (storedHash.equals(inputHash)) {
                    return rs.getInt("id");
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return -1;
    }

    public static List<Conversation> getConversations(String username) {

        List<Conversation> list = new ArrayList<>();

        String sql = """
        SELECT * FROM conversations
        WHERE user1 = ? OR user2 = ?
        ORDER BY last_timestamp DESC
    """;

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, username);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Conversation conv = new Conversation(
                        rs.getInt("id"),
                        rs.getString("user1"),
                        rs.getString("user2"),
                        rs.getString("last_message")
                );
                list.add(conv);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static List<String> searchUsers(String keyword) {
        String sql = "SELECT username FROM users WHERE username LIKE ?";
        List<String> results = new ArrayList<>();

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + keyword + "%");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(rs.getString("username"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }

    // Save a message
    public static void saveMessage(int senderId, int receiverId, String message) {
        String sql = "INSERT INTO messages(sender_id, receiver_id, message) VALUES(?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Load chat history between two users
    public static List<Message> getMessagesBetween(int userId1, int userId2) {
        List<Message> messages = new ArrayList<>();
        String sql = """
        SELECT sender_id, message, timestamp FROM messages
        WHERE (sender_id = ? AND receiver_id = ?)
           OR (sender_id = ? AND receiver_id = ?)
        ORDER BY timestamp ASC
    """;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1); pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2); pstmt.setInt(4, userId1);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                messages.add(new Message(
                        rs.getInt("sender_id"),
                        rs.getString("message"),
                        rs.getString("timestamp")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    public static void createConversationTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS conversations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            user1 TEXT NOT NULL,
            user2 TEXT NOT NULL,
            last_message TEXT,
            last_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            UNIQUE(user1, user2)
        );
    """;
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void upsertConversation(String user1, String user2, String lastMessage) {
        String u1 = user1.compareTo(user2) < 0 ? user1 : user2;
        String u2 = user1.compareTo(user2) < 0 ? user2 : user1;

        String sql = """
        INSERT INTO conversations(user1, user2, last_message, last_timestamp)
        VALUES(?, ?, ?, CURRENT_TIMESTAMP)
        ON CONFLICT(user1, user2)
        DO UPDATE SET last_message = excluded.last_message,
                      last_timestamp = CURRENT_TIMESTAMP
    """;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, u1);
            pstmt.setString(2, u2);
            pstmt.setString(3, lastMessage);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Image
    public static byte[] getProfilePicture(int userId) {
        String sql = "SELECT profile_pic FROM users WHERE id = ?";

        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getBytes("profile_pic"); // return bytes
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }


    // Group Messages
    public static void createGroupTables() {
        String groups = """
        CREATE TABLE IF NOT EXISTS groups (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            created_by TEXT NOT NULL
        );
    """;
        String members = """
        CREATE TABLE IF NOT EXISTS group_members (
            group_id INTEGER,
            username TEXT,
            PRIMARY KEY(group_id, username),
            FOREIGN KEY(group_id) REFERENCES groups(id)
        );
    """;
        String messages = """
        CREATE TABLE IF NOT EXISTS group_messages (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            group_id INTEGER,
            sender TEXT NOT NULL,
            message TEXT NOT NULL,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY(group_id) REFERENCES groups(id)
        );
    """;
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(groups);
            stmt.execute(members);
            stmt.execute(messages);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static int createGroup(String name, String createdBy, List<String> members) {
        String sql = "INSERT INTO groups(name, created_by) VALUES(?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, createdBy);
            pstmt.executeUpdate();
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) {
                int groupId = keys.getInt(1);
                // Insert all members including the creator
                String memberSql = "INSERT INTO group_members(group_id, username) VALUES(?, ?)";
                try (PreparedStatement mp = conn.prepareStatement(memberSql)) {
                    mp.setInt(1, groupId);
                    mp.setString(2, createdBy);
                    mp.executeUpdate();
                    for (String member : members) {
                        mp.setString(2, member);
                        mp.executeUpdate();
                    }
                }
                return groupId;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public static void saveGroupMessage(int groupId, String sender, String message) {
        String sql = "INSERT INTO group_messages(group_id, sender, message) VALUES(?, ?, ?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            pstmt.setString(2, sender);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public static List<GroupMessage> getGroupMessages(int groupId) {
        List<GroupMessage> list = new ArrayList<>();
        String sql = "SELECT sender, message, timestamp FROM group_messages WHERE group_id = ? ORDER BY timestamp ASC";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new GroupMessage(rs.getString("sender"), rs.getString("message"), rs.getString("timestamp")));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<String[]> getGroupsForUser(String username) {
        List<String[]> list = new ArrayList<>();
        String sql = """
        SELECT g.id, g.name FROM groups g
        JOIN group_members gm ON g.id = gm.group_id
        WHERE gm.username = ?
    """;
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next())
                list.add(new String[]{ String.valueOf(rs.getInt("id")), rs.getString("name") });
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public static List<String> getGroupMembers(int groupId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT username FROM group_members WHERE group_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(rs.getString("username"));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}

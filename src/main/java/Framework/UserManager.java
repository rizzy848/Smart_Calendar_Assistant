package Framework;

import entity.User;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages multiple users and their sessions.
 * Stores user data in a simple file-based system.
 */
public class UserManager {

    private static final String USERS_FILE = "users.dat";
    private static final String TOKENS_BASE_DIR = "tokens";

    private Map<String, User> users;  // userId -> User
    private User currentUser;

    public UserManager() {
        this.users = new HashMap<>();
        loadUsers();
        ensureTokensDirectory();
    }

    /**
     * Create or register a new user
     */
    public User registerUser(String username, String email) {
        // Check if email already exists
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                System.out.println("⚠️  User with this email already exists!");
                return user;
            }
        }

        // Create new user
        String userId = generateUserId();
        User newUser = new User(userId, username, email);
        users.put(userId, newUser);

        // Create user's token directory
        createUserTokenDirectory(userId);

        saveUsers();

        System.out.println("✅ User registered: " + username + " (" + email + ")");
        return newUser;
    }

    /**
     * Login user by email
     */
    public User loginUser(String email) {
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                user.login();
                currentUser = user;
                saveUsers();
                System.out.println("✅ Welcome back, " + user.getDisplayName() + "!");
                return user;
            }
        }

        System.out.println("❌ User not found with email: " + email);
        return null;
    }

    /**
     * Logout current user
     */
    public void logoutCurrentUser() {
        if (currentUser != null) {
            currentUser.logout();
            saveUsers();
            System.out.println("👋 Goodbye, " + currentUser.getDisplayName() + "!");
            currentUser = null;
        }
    }

    /**
     * Get current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Check if a user is logged in
     */
    public boolean hasCurrentUser() {
        return currentUser != null;
    }

    /**
     * List all registered users (for user selection)
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /**
     * Delete a user and their data
     */
    public boolean deleteUser(String userId) {
        User user = users.remove(userId);
        if (user != null) {
            // Delete user's token directory
            deleteUserTokenDirectory(userId);
            saveUsers();
            System.out.println("🗑️  User deleted: " + user.getDisplayName());

            if (currentUser != null && currentUser.getUserId().equals(userId)) {
                currentUser = null;
            }
            return true;
        }
        return false;
    }

    // ===== PERSISTENCE =====

    private void loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            System.out.println("📝 No existing users found. Starting fresh.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Map<String, User> loadedUsers = (Map<String, User>) ois.readObject();
            users = loadedUsers;
            System.out.println("✅ Loaded " + users.size() + " user(s)");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("⚠️  Could not load users: " + e.getMessage());
            users = new HashMap<>();
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (IOException e) {
            System.err.println("❌ Could not save users: " + e.getMessage());
        }
    }

    // ===== TOKEN DIRECTORY MANAGEMENT =====

    private void ensureTokensDirectory() {
        try {
            Files.createDirectories(Paths.get(TOKENS_BASE_DIR));
        } catch (IOException e) {
            System.err.println("Could not create tokens directory: " + e.getMessage());
        }
    }

    private void createUserTokenDirectory(String userId) {
        try {
            Path userTokensPath = Paths.get(TOKENS_BASE_DIR, userId);
            Files.createDirectories(userTokensPath);
        } catch (IOException e) {
            System.err.println("Could not create user token directory: " + e.getMessage());
        }
    }

    private void deleteUserTokenDirectory(String userId) {
        try {
            Path userTokensPath = Paths.get(TOKENS_BASE_DIR, userId);
            if (Files.exists(userTokensPath)) {
                Files.walk(userTokensPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                System.err.println("Could not delete: " + path);
                            }
                        });
            }
        } catch (IOException e) {
            System.err.println("Could not delete user tokens: " + e.getMessage());
        }
    }

    // ===== UTILITY =====

    private String generateUserId() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Display all users (for debugging/admin)
     */
    public void displayAllUsers() {
        System.out.println("\n===== Registered Users =====");
        if (users.isEmpty()) {
            System.out.println("No users registered yet.");
        } else {
            int index = 1;
            for (User user : users.values()) {
                System.out.printf("%d. %s (%s)%s\n",
                        index++,
                        user.getDisplayName(),
                        user.getEmail(),
                        user.isAuthenticated() ? " [LOGGED IN]" : "");
            }
        }
        System.out.println("============================\n");
    }
}
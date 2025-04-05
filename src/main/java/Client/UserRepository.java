package Client;

import java.io.*;
import java.util.Base64;

public class UserRepository {
    private static final String CSV_FILE = "users.csv";
    private static final String DELIMITER = ",";

    public static void saveUser(User user) throws IOException {
        File file = new File(CSV_FILE);
        boolean fileExists = file.exists();

        // Check if username already exists
        if (fileExists) {
            try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(DELIMITER);
                    if (parts.length > 0 && parts[0].equals(user.getUsername())) {
                        throw new IOException("Username already exists");
                    }
                }
            }
        }

        // Save the new user with plain text password
        try (PrintWriter out = new PrintWriter(new FileWriter(CSV_FILE, true))) {
            if (!fileExists) {
                out.println("username,password"); // Header
            }
            out.println(user.getUsername() + DELIMITER + user.getPassword());
        }
    }

    public static User findUserByUsername(String username) throws IOException {
        File file = new File(CSV_FILE);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(CSV_FILE))) {
            String line;
            reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(DELIMITER);
                if (parts.length >= 2 && parts[0].equals(username)) {
                    return new User(parts[0], parts[1]);
                }
            }
        }
        return null;
    }
}
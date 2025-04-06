package Client;

// LoginSignupFrame.java

import java.awt.CardLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public class LoginSignupFrame extends JFrame {
    private JPanel cards;
    private JPanel loginPanel;
    private JPanel signupPanel;

    public LoginSignupFrame() {
        setTitle("Login / Signup");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cards = new JPanel(new CardLayout());
        loginPanel = createLoginPanel();
        signupPanel = createSignupPanel();

        cards.add(loginPanel, "login");
        cards.add(signupPanel, "signup");

        add(cards);
        setVisible(true);
    }

    private boolean isPasswordStrong(String password) {
        // At least 8 characters, one uppercase, one lowercase, one digit, one special character
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(6, 1, 5, 5));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JLabel signupLink = new JLabel("<html><a href='#'>Don't have an account? Sign up</a></html>");
        JCheckBox showPassword = new JCheckBox("Show Password");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(showPassword);
        panel.add(loginButton);
        panel.add(signupLink);

        showPassword.addActionListener(e -> {
            passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '•');
        });

        loginButton.addActionListener(e -> handleLogin(usernameField, passwordField));
        signupLink.addMouseListener(createSwitchPanelListener("signup"));

        return panel;
    }

    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new GridLayout(8, 1, 5, 5));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JButton signupButton = new JButton("Sign Up");
        JLabel loginLink = new JLabel("<html><a href='#'>Already have an account? Login</a></html>");
        JCheckBox showPassword = new JCheckBox("Show Password");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(showPassword);
        panel.add(signupButton);
        panel.add(loginLink);

        showPassword.addActionListener(e -> {
            char echo = showPassword.isSelected() ? (char) 0 : '•';
            passwordField.setEchoChar(echo);
            confirmPasswordField.setEchoChar(echo);
        });

        signupButton.addActionListener(e -> handleSignup(usernameField, passwordField, confirmPasswordField));
        loginLink.addMouseListener(createSwitchPanelListener("login"));

        return panel;
    }

    private void handleLogin(JTextField usernameField, JPasswordField passwordField) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        try {
            User user = UserRepository.findUserByUsername(username);
            if (user != null && user.getPassword().equals(password)) {
                dispose();
                new Client(user.getUsername());
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password.");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error accessing user data.");
        }
    }

    private void handleSignup(JTextField usernameField, JPasswordField passwordField, JPasswordField confirmPasswordField) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.");
            return;
        }

        if (!isPasswordStrong(password)) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters, contain uppercase, lowercase, number, and special character.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.");
            return;
        }

        try {
            User existing = UserRepository.findUserByUsername(username);
            if (existing != null) {
                JOptionPane.showMessageDialog(this, "Username already exists.");
                return;
            }

            UserRepository.saveUser(new User(username, password));
            JOptionPane.showMessageDialog(this, "Account created successfully! Please log in.");

            // Clear fields and redirect to login
            usernameField.setText("");
            passwordField.setText("");
            confirmPasswordField.setText("");
            CardLayout cl = (CardLayout) cards.getLayout();
            cl.show(cards, "login");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private MouseAdapter createSwitchPanelListener(String panelName) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CardLayout cl = (CardLayout) cards.getLayout();
                cl.show(cards, panelName);
            }
        };
    }
}

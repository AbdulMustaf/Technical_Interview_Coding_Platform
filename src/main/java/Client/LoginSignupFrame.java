package Client;

// LoginSignupFrame.java

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class LoginSignupFrame extends JFrame {
    private JPanel cards;
    private JPanel loginPanel;
    private JPanel signupPanel;

    public LoginSignupFrame() {
        setTitle("Login / Signup");
        setSize(400, 300);
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

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");
        JLabel signupLink = new JLabel("<html><a href='#'>Don't have an account? Sign up</a></html>");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(loginButton);
        panel.add(signupLink);

        loginButton.addActionListener(e -> handleLogin(usernameField, passwordField));
        signupLink.addMouseListener(createSwitchPanelListener("signup"));

        return panel;
    }

    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 5, 5));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();
        JButton signupButton = new JButton("Sign Up");
        JLabel loginLink = new JLabel("<html><a href='#'>Already have an account? Login</a></html>");

        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Confirm Password:"));
        panel.add(confirmPasswordField);
        panel.add(signupButton);
        panel.add(loginLink);

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
                JOptionPane.showMessageDialog(this, "Invalid credentials");
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error accessing user data");
        }
    }

    private void handleSignup(JTextField usernameField, JPasswordField passwordField, JPasswordField confirmPasswordField) {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match");
            return;
        }

        try {
            UserRepository.saveUser(new User(username, password));
            JOptionPane.showMessageDialog(this, "Signup successful! Please login.");
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

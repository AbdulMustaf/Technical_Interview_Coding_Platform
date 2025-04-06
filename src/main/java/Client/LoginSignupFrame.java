package Client;


// LoginSignupFrame.java

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

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
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        return password.matches(regex);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(30, 60, 30, 60));
        panel.setBackground(new Color(245, 245, 245));

        // Title
        JLabel title = new JLabel("Login");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        formPanel.add(usernameLabel, gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("Password:");
        formPanel.add(passwordLabel, gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Show Password
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JCheckBox showPassword = new JCheckBox("Show Password");
        formPanel.add(showPassword, gbc);

        // Login Button
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton loginButton = new JButton("Login");
        loginButton.setPreferredSize(new Dimension(100, 30));
        formPanel.add(loginButton, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Signup Link
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(245, 245, 245));
        JLabel signupLink = new JLabel("<html><a href='#'>Don't have an account? Sign up</a></html>");
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        signupLink.setForeground(Color.BLUE);
        bottomPanel.add(signupLink);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
        showPassword.addActionListener(e -> {
            passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : '•');
        });
        loginButton.addActionListener(e -> handleLogin(usernameField, passwordField));
        signupLink.addMouseListener(createSwitchPanelListener("signup"));

        return panel;
    }

    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new EmptyBorder(30, 60, 30, 60));
        panel.setBackground(new Color(245, 245, 245));

        // Title
        JLabel title = new JLabel("Sign Up");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(title, BorderLayout.NORTH);

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 245, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Username
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        JTextField usernameField = new JTextField(20);
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        JPasswordField passwordField = new JPasswordField(20);
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Confirm Password:"), gbc);

        gbc.gridx = 1;
        JPasswordField confirmPasswordField = new JPasswordField(20);
        formPanel.add(confirmPasswordField, gbc);

        // Show Password
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JCheckBox showPassword = new JCheckBox("Show Password");
        formPanel.add(showPassword, gbc);

        // Signup Button
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton signupButton = new JButton("Sign Up");
        signupButton.setPreferredSize(new Dimension(100, 30));
        formPanel.add(signupButton, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Login Link
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(new Color(245, 245, 245));
        JLabel loginLink = new JLabel("<html><a href='#'>Already have an account? Login</a></html>");
        loginLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLink.setForeground(Color.BLUE);
        bottomPanel.add(loginLink);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        // Listeners
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


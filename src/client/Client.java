package client;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Client {
    private PrintWriter out;
    private JTextArea editorArea;
    private JTextArea chatArea;
    private boolean isUpdatingFromServer = false;

    public Client() {
        try {
            Socket socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("[CHAT]")) {
                            updateChat(message.substring(6));
                        } else {
                            updateEditor(message);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Client read error: " + e.getMessage());
                }
            }).start();
        } catch (Exception e) {
            System.err.println("Client connection error: " + e.getMessage());
        }
        buildGUI();
    }

    private void buildGUI() {
        JFrame frame = new JFrame("Interview Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
    
        // Initialize editorArea FIRST (no typos)
        editorArea = new JTextArea(); // Ensure this is assigned to the instance variable, not a local variable
    
        // Add document listener AFTER initialization
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { triggerUpdate(); }
            @Override
            public void removeUpdate(DocumentEvent e) { triggerUpdate(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JTextField chatInput = new JTextField();
        chatInput.addActionListener((ActionEvent e) -> {
            out.println("[CHAT]" + chatInput.getText());
            chatInput.setText("");
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(editorArea), BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        panel.add(chatPanel, BorderLayout.EAST);
        frame.add(panel);
        frame.setVisible(true);
    }

    private void triggerUpdate() {
        if (!isUpdatingFromServer) {
            out.println(editorArea.getText());
        }
    }

    private void updateEditor(String text) {
        SwingUtilities.invokeLater(() -> {
            isUpdatingFromServer = true;
            editorArea.setText(text);
            isUpdatingFromServer = false;
        });
    }

    private void updateChat(String text) {
        SwingUtilities.invokeLater(() -> chatArea.append(text + "\n"));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}
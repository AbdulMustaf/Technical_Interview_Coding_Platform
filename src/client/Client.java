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
    private File currentFile;

    public Client() {
        try {
            Socket socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                    String message;
                    while ((message = in.readLine()) != null) {
                        // Logic for a editor or chat message
                        if (message.startsWith("CHAT:")) {
                            updateChat(message.substring(5));
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
        frame.setSize(800, 500);

        //File I/O menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem openItem = new JMenuItem("Open");
        openItem.addActionListener(this::openFile);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.addActionListener(this::saveFile);
        JMenuItem saveAsItem = new JMenuItem("Save As");
        saveAsItem.addActionListener(this::saveFileAs);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        menuBar.add(fileMenu);
        frame.setJMenuBar(menuBar);

        editorArea = new JTextArea();
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { triggerCodeUpdate(); }
            @Override
            public void removeUpdate(DocumentEvent e) { triggerCodeUpdate(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        // Chat GUI functionality
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        JTextField chatInput = new JTextField("Type Here");
        chatInput.setForeground(Color.GRAY);
        // Listener to properly display "Type Here" for the user
        chatInput.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (chatInput.getText().equals("Type Here")) {
                    chatInput.setText("");
                    chatInput.setForeground(Color.BLACK);
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                if (chatInput.getText().isEmpty()) {
                    chatInput.setText("Type Here");
                    chatInput.setForeground(Color.GRAY);
                }
            }
        });
        // Chat message sending from GUI
        chatInput.addActionListener((ActionEvent e) -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty() && !msg.equals("Type Here")) {
                out.println("CHAT:" + msg);
                chatInput.setText("");
            }
        });

        // GUI layout
        JLabel editorLabel = new JLabel("Editor");
        JLabel chatLabel = new JLabel("Chat");
        
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(chatLabel, BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(editorLabel, BorderLayout.NORTH);
        editorPanel.add(new JScrollPane(editorArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, chatPanel);
        splitPane.setDividerLocation(500);

        frame.add(splitPane);
        frame.setVisible(true);
    }

    // GUI functions and server interaction

    private void triggerCodeUpdate() {
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

    // Opens a file from client computer
    private void openFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(currentFile))) {
                StringBuilder fileContent = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    fileContent.append(line).append("\n");
                }
                editorArea.setText(fileContent.toString());
                out.println(editorArea.getText());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, "Error reading file: " + ex.getMessage());
            }
        }
    }

    // Save and save as for editor
    private void saveFile(ActionEvent e) {
        if (currentFile != null) {
            writeFile(currentFile);
        } else {
            saveFileAs(e);
        }
    }

    // Helper to saveFile(); saves editor text as
    private void saveFileAs(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            writeFile(currentFile);
        }
    }

    // Helper to saveFile() and saveFileAs() to read in editor text
    private void writeFile(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(editorArea.getText());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error saving file: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }
}

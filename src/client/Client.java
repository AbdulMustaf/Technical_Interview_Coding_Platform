package client;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.swing.*;
import javax.swing.event.*;

public class Client {
    private PrintWriter out; // For sending commands to server
    private Socket socket;
    private JTextArea editorArea;
    private JTextArea chatArea;
    private boolean isUpdatingFromServer = false;
    private File currentFile;
    private volatile boolean isConnected = true; // For updating status of client connection to server

    public Client() {
        try {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);

            JFrame frame = buildGUI();
            // If window is closed; shutdown(close resources)
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    shutdownClient();
                }
            });

            startServerListenerThread(); // Multi-threads server commands and handles logic of commands and function calling
        } catch (Exception e) {
            System.err.println("Client connection error: " + e.getMessage());
            shutdownClient();
        }
    }

    private JFrame buildGUI() {
        JFrame frame = new JFrame("Interview Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        // File I/O menu
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

        // Text editor changes handling
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
        
        JTextField chatInput = createChatInput(); // Creates chat input box
        
        // Adding editor and chat to the frame
        JSplitPane splitPane = createSplitPane(chatScrollPane, chatInput); // Formatted and labeled splitpane
        frame.add(splitPane);
        
        frame.setVisible(true);
        return frame;
    }

    // Functionality for chat inout field
    private JTextField createChatInput() {
        JTextField chatInput = new JTextField("Type Here");
        chatInput.setForeground(Color.GRAY);
        // Displaying "Type Here" on chat input field
        chatInput.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (chatInput.getText().equals("Type Here")) {
                    chatInput.setText("");
                    chatInput.setForeground(Color.BLACK);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (chatInput.getText().isEmpty()) {
                    chatInput.setText("Type Here");
                    chatInput.setForeground(Color.GRAY);
                }
            }
        });
        
        // Taking message from chat input field and sending to chat box and server
        chatInput.addActionListener(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty() && !msg.equals("Type Here")) {
                out.println("CHAT:" + msg);
                updateChat("You: " + msg);
                chatInput.setText("");
            }
        });
        return chatInput;
    }

    // Makes labeled and formatted splitpane for editor and chat
    private JSplitPane createSplitPane(JScrollPane chatScrollPane, JTextField chatInput) {
        // JPanels for editor and chat added
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JLabel("Chat"), BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.add(new JLabel("Editor"), BorderLayout.NORTH);
        editorPanel.add(new JScrollPane(editorArea), BorderLayout.CENTER);

        // Editor and caht sizing
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, chatPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setContinuousLayout(true);

        // Set initial divider location
        SwingUtilities.invokeLater(() -> {
            try {
                splitPane.setDividerLocation(0.7);
            } catch (IllegalArgumentException e) {
                splitPane.setDividerLocation(500);
            }
        });

        // Window resize listener
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                if (splitPane.getWidth() > 0) {
                    splitPane.setDividerLocation(0.7);
                }
            }
        });
        
        return splitPane;
    }

    // Server command recieving and handling logic
    private void startServerListenerThread() {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while (isConnected && (message = in.readLine()) != null) {
                    if (message.startsWith("CHAT:")) { // Chat messages sent to chat
                        updateChat(message.substring(5));

                    } else if (message.startsWith("EDITOR:")) { // Editor updates to editor
                        String encodedContent = message.substring(7);
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
                        String decodedContent = new String(decodedBytes, StandardCharsets.UTF_8);
                        updateEditor(decodedContent);
                    }
                }
            } catch (SocketException e) {
                if (isConnected) {
                    SwingUtilities.invokeLater(() -> {
                        updateChat("Connection to server lost!");
                        disableInputs();
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    updateChat("Error: " + e.getMessage());
                });
            } finally {
                shutdownClient(); // Closes resources.
            }
        }).start();
    }

    // Ensures all resources close to stop memory leak(s)
    private void shutdownClient() {
        isConnected = false;
        try {
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing resources: " + e.getMessage());
        }
    }

    private void disableInputs() {
        editorArea.setEnabled(false);
    }

    // If client chages editor, it is sent to server
    private void triggerCodeUpdate() {
        if (!isUpdatingFromServer && isConnected) {
            String content = editorArea.getText();
            String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            out.println("EDITOR:" + encoded);
        }
    }

    // Changes text displayed on editor
    private void updateEditor(String text) {
        SwingUtilities.invokeLater(() -> {
            isUpdatingFromServer = true;
            editorArea.setText(text);
            isUpdatingFromServer = false;
        });
    }

    // Add new chat message from server to chat box
    private void updateChat(String text) {
        SwingUtilities.invokeLater(() -> chatArea.append(text + "\n"));
    }

    // Opens a file from client computer and puts it onto the editor
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
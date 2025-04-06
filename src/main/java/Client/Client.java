package Client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Client {
    private PrintWriter out; // For sending commands to server
    private Socket socket;
    private JTextArea editorArea;
    private JTextArea chatArea;
    private boolean isUpdatingFromServer = false;
    private File currentFile;
    private volatile boolean isConnected = true; // For tracking client connection status
    private final String username;

    // Unique ID for this client
    private final String clientId = "client-" + new Random().nextInt(100000);
    // Maps to store remote clients' caret positions, colors, and focus states
    private final Map<String, Integer> remoteCursors = new ConcurrentHashMap<>();
    private final Map<String, Color> remoteCursorColors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> remoteFocusStates = new ConcurrentHashMap<>();

    private RemoteCursorOverlay overlay;
    
    // Timer label (will show the shared timer state)
    private JLabel timerLabel;

    public Client(String username) {
        this.username = username;
        try {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);

            // Send client ID and username to server
            out.println("CLIENT_ID:" + clientId);
            out.println("USERNAME:" + username);

            JFrame frame = buildGUI();

            // Send window activation events to track focus state
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowDeactivated(WindowEvent e) {
                    out.println("FOCUS:" + clientId + ":lost");
                }
               
                @Override
                public void windowActivated(WindowEvent e) {
                    out.println("FOCUS:" + clientId + ":gained");
                }
            });

            // When window is closed, shutdown client and release resources
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    shutdownClient();
                }
            });

            startServerListenerThread(); // Start thread to handle server messages

            // Send local caret position updates to server
            editorArea.addCaretListener(e -> {
                if (isConnected && !isUpdatingFromServer) {
                    int pos = e.getDot();
                    out.println("CURSOR:" + clientId + ":" + pos);
                }
            });
        } catch (Exception e) {
            System.err.println("Client connection error: " + e.getMessage());
            shutdownClient();
        }
    }

    private JFrame buildGUI() {
        JFrame frame = new JFrame("Interview Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 500);

        // File I/O menu setup
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

        // Create header panel with timer and control buttons at the top-right
        JPanel headerPanel = new JPanel(new BorderLayout());
        JPanel timerPanel = new JPanel(); // uses FlowLayout by default
        timerLabel = new JLabel("Timer: 00:00");
        JButton startButton = new JButton("Start ⏵");
        JButton pauseButton = new JButton("Pause ⏸");
        JButton resetButton = new JButton("Reset 🔁");

        // When buttons are clicked, send timer control commands to server
        startButton.addActionListener(e -> out.println("TIMER_START"));
        pauseButton.addActionListener(e -> out.println("TIMER_PAUSE"));
        resetButton.addActionListener(e -> out.println("TIMER_RESET"));

        timerPanel.add(timerLabel);
        timerPanel.add(startButton);
        timerPanel.add(pauseButton);
        timerPanel.add(resetButton);
        headerPanel.add(timerPanel, BorderLayout.EAST);

        // Create editor area with document change listener
        editorArea = new JTextArea();
        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { triggerCodeUpdate(); }
            @Override
            public void removeUpdate(DocumentEvent e) { triggerCodeUpdate(); }
            @Override
            public void changedUpdate(DocumentEvent e) {}
        });

        // Setup layered pane for editor and cursor overlay
        JLayeredPane layeredPane = new JLayeredPane();
        JScrollPane editorScrollPane = new JScrollPane(editorArea);
        editorScrollPane.setBounds(0, 0, 550, 500);
        layeredPane.add(editorScrollPane, JLayeredPane.DEFAULT_LAYER);

        // Initialize remote cursor overlay
        overlay = new RemoteCursorOverlay(editorArea, remoteCursors, remoteCursorColors, remoteFocusStates);
        overlay.setOpaque(false);
        overlay.setBounds(editorScrollPane.getBounds());
        layeredPane.add(overlay, JLayeredPane.PALETTE_LAYER);

        // Chat area setup
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);

        JTextField chatInput = createChatInput();

        // Build chat panel with input field
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.add(new JLabel("Chat"), BorderLayout.NORTH);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);
        chatPanel.add(chatInput, BorderLayout.SOUTH);

        // Create split pane for editor and chat
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, layeredPane, chatPanel);
        splitPane.setResizeWeight(0.7);
        splitPane.setContinuousLayout(true);
       
        // Set initial divider location and resize handling
        SwingUtilities.invokeLater(() -> {
            try {
                splitPane.setDividerLocation(0.7);
            } catch (IllegalArgumentException e) {
                splitPane.setDividerLocation(500);
            }
        });
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (splitPane.getWidth() > 0) {
                    splitPane.setDividerLocation(0.7);
                    // Adjust overlay bounds on window resize
                    editorScrollPane.setBounds(0, 0, splitPane.getWidth() * 7 / 10, splitPane.getHeight());
                    overlay.setBounds(editorScrollPane.getBounds());
                }
            }
        });

        // Set frame layout and add header and main split pane
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(headerPanel, BorderLayout.NORTH);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);

        frame.setVisible(true);
        return frame;
    }

    // Creates chat input field with placeholder text and message sending
    private JTextField createChatInput() {
        JTextField chatInput = new JTextField("Type Here");
        chatInput.setForeground(Color.GRAY);
   
        // Handle placeholder text display
        chatInput.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (chatInput.getText().equals("Type Here")) {
                    chatInput.setText("");
                    chatInput.setForeground(Color.BLACK);
                }
            }
   
            @Override
            public void focusLost(FocusEvent e) {
                if (chatInput.getText().isEmpty()) {
                    chatInput.setText("Type Here");
                    chatInput.setForeground(Color.GRAY);
                }
            }
        });
   
        // Send chat messages on Enter key
        chatInput.addActionListener(e -> {
            String msg = chatInput.getText().trim();
            if (!msg.isEmpty() && !msg.equals("Type Here")) {
                out.println("CHAT:" + username + ":" + msg); // Send without extra space
                updateChat("You: " + msg);
                chatInput.setText("");
            }
        });
   
        return chatInput;
    }
   
    // Thread to listen for server messages and handle different command types
    private void startServerListenerThread() {
        new Thread(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                String message;
                while (isConnected && (message = in.readLine()) != null) {
                    if (message.startsWith("CHAT:")) {
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String senderUsername = parts[1];
                            String chatMessage = parts[2];
                            updateChat(senderUsername + ": " + chatMessage);
                        } else {
                            updateChat("Unknown: " + message.substring(5));
                        }
                    } else if (message.startsWith("EDITOR:")) {
                        // Handle editor updates
                        String encodedContent = message.substring(7);
                        byte[] decodedBytes = Base64.getDecoder().decode(encodedContent);
                        String decodedContent = new String(decodedBytes, StandardCharsets.UTF_8);
                        updateEditor(decodedContent);
                    } else if (message.startsWith("CURSOR:")) {
                        // Handle remote cursor positions with username
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String remoteUsername = parts[1];
                            if (!remoteUsername.equals(this.username)) { // Ignore own cursor
                                try {
                                    int pos = Integer.parseInt(parts[2]);
                                    remoteCursorColors.putIfAbsent(remoteUsername,
                                        new Color(new Random().nextInt(256),
                                        new Random().nextInt(256),
                                        new Random().nextInt(256)));

                                    remoteCursors.put(remoteUsername, pos);
                                    overlay.repaint();
                                } catch (NumberFormatException ex) {
                                    System.err.println("Invalid cursor position: " + parts[2]);
                                }
                            }
                        }
                    } else if (message.startsWith("FOCUS:")) {
                        // Handle focus state changes
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3) {
                            String remoteUsername = parts[1];
                            if (!remoteUsername.equals(this.username)) {
                                boolean inFocus = parts[2].equalsIgnoreCase("gained");
                                remoteFocusStates.put(remoteUsername, inFocus);
                                overlay.repaint();
                            }
                        }
                    } else if (message.startsWith("DISCONNECT:")) {
                        // Handle client disconnections
                        String disconnectedUser = message.substring("DISCONNECT:".length());
                        remoteCursors.remove(disconnectedUser);
                        remoteCursorColors.remove(disconnectedUser);
                        remoteFocusStates.remove(disconnectedUser);
                        overlay.repaint();
                    } else if (message.startsWith("TIMER_UPDATE:")) {
                        // Format: TIMER_UPDATE:<elapsedSeconds>:<status>
                        String[] parts = message.split(":");
                        if (parts.length == 3) {
                            try {
                                int elapsed = Integer.parseInt(parts[1]);
                                int minutes = elapsed / 60;
                                int seconds = elapsed % 60;
                                SwingUtilities.invokeLater(() -> 
                                    timerLabel.setText(String.format("Timer: %02d:%02d", minutes, seconds))
                                );
                            } catch (NumberFormatException nfe) {
                                // Ignore parsing error
                            }
                        }
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
                shutdownClient();
            }
        }).start();
    }

    // Close client connection and clean up resources
    private void shutdownClient() {
        if (isConnected && out != null) {
            out.println("DISCONNECT:" + clientId);
        }
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

    // Send editor content updates to server
    private void triggerCodeUpdate() {
        if (!isUpdatingFromServer && isConnected) {
            String content = editorArea.getText();
            String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
            out.println("EDITOR:" + encoded);
        }
    }

    // Update editor content from server
    private void updateEditor(String text) {
        SwingUtilities.invokeLater(() -> {
            isUpdatingFromServer = true;
            editorArea.setText(text);
            isUpdatingFromServer = false;
        });
    }

    // Append messages to chat area
    private void updateChat(String text) {
        SwingUtilities.invokeLater(() -> chatArea.append(text + "\n"));
    }

    // Open file and load into editor
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

    // Save current file or prompt for new file
    private void saveFile(ActionEvent e) {
        if (currentFile != null) {
            writeFile(currentFile);
        } else {
            saveFileAs(e);
        }
    }

    // Save file with new name/location
    private void saveFileAs(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(null);
        if (option == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            writeFile(currentFile);
        }
    }

    // Write editor content to file
    private void writeFile(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(editorArea.getText());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(null, "Error saving file: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginSignupFrame());
    }
}

/**
 * Overlay component to draw remote client cursors.
 * Only draws a cursor if the remote client is in focus.
 */
class RemoteCursorOverlay extends JComponent {
    private final JTextArea editor;
    private final Map<String, Integer> remoteCursors;
    private final Map<String, Color> remoteCursorColors;
    private final Map<String, Boolean> remoteFocusStates;

    public RemoteCursorOverlay(JTextArea editor,
                               Map<String, Integer> remoteCursors,
                               Map<String, Color> remoteCursorColors,
                               Map<String, Boolean> remoteFocusStates) {
        this.editor = editor;
        this.remoteCursors = remoteCursors;
        this.remoteCursorColors = remoteCursorColors;
        this.remoteFocusStates = remoteFocusStates;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw all remote cursors that are in focus
        for (Map.Entry<String, Integer> entry : remoteCursors.entrySet()) {
            String username = entry.getKey();
            boolean inFocus = remoteFocusStates.getOrDefault(username, true);
            if (!inFocus) continue;

            int pos = entry.getValue();
            try {
                Rectangle r = editor.modelToView(pos);
                if (r != null) {
                    Color color = remoteCursorColors.get(username);
                    g.setColor(color);

                    // draw cursor
                    int x = r.x;
                    int y = r.y;
                    int height = r.height;
                    g.fillRect(x, y, 2, height);

                    // draw username
                    g.drawString(username, x + 5, y - 5);
                }
            } catch (Exception e) {
                // Invalid cursor position, skip drawing
            }
        }
    }
}

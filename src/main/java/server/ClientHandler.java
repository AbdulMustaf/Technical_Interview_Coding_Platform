package server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter out;
    private final List<ClientHandler> clients;
    private String clientId;
    private String username;

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = writer;

            // Send latest editor content to new clients
            if (Server.latestEditorContent != null) {
                this.out.println(Server.latestEditorContent);
            }

            String message;
            while ((message = in.readLine()) != null) {
                // Handle client registration messages
                if (message.startsWith("CLIENT_ID:")) {
                    this.clientId = message.substring("CLIENT_ID:".length());
                } else if (message.startsWith("USERNAME:")) {
                    this.username = message.substring("USERNAME:".length());
                    Server.clientIdToUsername.put(clientId, username);
                }
                // Update server's latest editor content if needed
                else if (message.startsWith("EDITOR:")) {
                    Server.latestEditorContent = message;
                    broadcast(message);
                }
                // Handle cursor position updates with username
                else if (message.startsWith("CURSOR:")) {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3 && clientId != null) {
                        String username = Server.clientIdToUsername.getOrDefault(clientId, "Unknown");
                        String updatedMessage = "CURSOR:" + username + ":" + parts[2];
                        broadcast(updatedMessage);
                    }
                }
                // Handle timer control commands
                else if (message.equals("TIMER_START")) {
                    Server.startTimer();
                } else if (message.equals("TIMER_PAUSE")) {
                    Server.pauseTimer();
                } else if (message.equals("TIMER_RESET")) {
                    Server.resetTimer();
                }
                // Broadcast other messages normally
                else {
                    broadcast(message);
                }
            }
        } catch (Exception e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            clients.remove(this);
            // Clean up user tracking
            if (clientId != null) {
                String removedUser = Server.clientIdToUsername.remove(clientId);
                if (removedUser != null) {
                    broadcast("DISCONNECT:" + removedUser);
                }
            }
            // Clear editor state when last client disconnects
            if (clients.isEmpty()) {
                Server.clearEditorState();
            }
        }
    }

    // Public method to send a message to this client
    public void sendMessage(String message) {
        out.println(message);
    }

    private void broadcast(String message) {
        for (ClientHandler client : clients) {
            if (client != this) {
                client.out.println(message);
            }
        }
    }
}

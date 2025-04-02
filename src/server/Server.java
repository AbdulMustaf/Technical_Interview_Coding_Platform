package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 8080;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static volatile String latestEditorContent = null; // Track latest editor state

    public static void clearEditorState() {
        latestEditorContent = null;
        System.out.println("All clients disconnected - editor state cleared");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientThread = new ClientHandler(clientSocket, clients);
                clients.add(clientThread);
                clientThread.start();
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
}
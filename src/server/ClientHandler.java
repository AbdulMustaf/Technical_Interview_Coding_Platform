

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {
    private final Socket socket;
    private PrintWriter out;
    private final List<ClientHandler> clients;

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        this.socket = socket;
        this.clients = clients;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Remove redeclaration of "out" (use the class field directly)
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) 
        ) {
            this.out = writer; // Assign to the class field
            String message;
            while ((message = in.readLine()) != null) {
                for (ClientHandler client : clients) {
                    if (client != this) {
                        client.out.println(message);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("ClientHandler error: " + e.getMessage());
        } finally {
            clients.remove(this);
        }
    }

    // Synchronized method to ensure thread-safe writes
    public void sendMessage(String message) {
        synchronized (out) {
            out.println(message);
        }
    }
}
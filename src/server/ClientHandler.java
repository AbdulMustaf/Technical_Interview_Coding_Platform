

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
            PrintWriter writer = new PrintWriter(socket.getOutputStream(), true) 
        ) {
            this.out = writer;

            // Send latest editor content to new clients
            if (Server.latestEditorContent != null) {
                this.out.println(Server.latestEditorContent);
            }

            String message;
            while ((message = in.readLine()) != null) {
                // Update server's latest editor content
                if (message.startsWith("EDITOR:")) {
                    Server.latestEditorContent = message;
                }
                // Broadcast to other clients
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
            // Clear editor state when last client disconnects
            if (clients.isEmpty()) {
                Server.clearEditorState();
            }
        }
    }
}
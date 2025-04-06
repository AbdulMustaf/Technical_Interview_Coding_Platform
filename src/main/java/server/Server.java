package server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static final int PORT = 8080;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    public static volatile String latestEditorContent = null; // Track latest editor state
    public static final ConcurrentHashMap<String, String> clientIdToUsername = new ConcurrentHashMap<>();

    // Shared timer state (in seconds)
    private static int timerElapsed = 0;
    private static boolean timerRunning = false;

    public static void clearEditorState() {
        latestEditorContent = null;
        System.out.println("All clients disconnected - editor state cleared");
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Start the timer thread to update timer state every second when running
            TimerThread timerThread = new TimerThread();
            timerThread.start();

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

    // Synchronized methods to control the timer
    public static synchronized void startTimer() {
        timerRunning = true;
        broadcastTimerUpdate();
    }

    public static synchronized void pauseTimer() {
        timerRunning = false;
        broadcastTimerUpdate();
    }

    public static synchronized void resetTimer() {
        timerElapsed = 0;
        timerRunning = false;
        broadcastTimerUpdate();
    }

    // Broadcast the current timer state to all clients
    public static synchronized void broadcastTimerUpdate() {
        String message = "TIMER_UPDATE:" + timerElapsed + ":" + (timerRunning ? "running" : "paused");
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Timer thread to tick every second when the timer is running
    static class TimerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Ignore interruptions
                }
                synchronized (Server.class) {
                    if (timerRunning) {
                        timerElapsed++;
                        broadcastTimerUpdate();
                    }
                }
            }
        }
    }
}

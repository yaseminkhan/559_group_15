package com.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

public class ConnectionCoordinator extends WebSocketServer {
    // 172.18.0.5
    private String currentPrimaryUrl = "ws://172.18.0.5:8887"; // Updated on leader change
    private WebSocketClient backendConnection;
    private final Queue<String> messageQueue = new ConcurrentLinkedQueue<>();

    public ConnectionCoordinator(InetSocketAddress address) {
        super(address);
        connectToPrimary();
    }

    private void connectToPrimary() {
        new Thread(() -> {
            int retries = 5;
            int attempt = 1;
            while (retries-- > 0) {
                try {
                    System.out.println("[Attempt " + attempt + "] Trying to connect to backend at: " + currentPrimaryUrl);
                    URI targetUri = new URI(currentPrimaryUrl);
                    System.out.println("[Attempt " + attempt + "] Parsed URI: " + targetUri);

                    backendConnection = new WebSocketClient(targetUri) {
                        @Override
                        public void onOpen(ServerHandshake handshake) {
                            System.out.println("Connected to primary at: " + currentPrimaryUrl);

                            // Flush queued messages after a short delay
                            new Timer().schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    while (!messageQueue.isEmpty()) {
                                        String queuedMessage = messageQueue.poll();
                                        backendConnection.send(queuedMessage);
                                        System.out.println("Flushed queued message: " + queuedMessage);
                                    }
                                    System.out.println("Backend ready. Queued messages sent.");
                                }
                            }, 1000);
                        }

                        @Override
                        public void onMessage(String message) {
                            broadcast(message); // Forward message from backend to all clients
                        }

                        @Override
                        public void onClose(int code, String reason, boolean remote) {
                            System.out.println("Primary connection closed: " + reason);
                        }

                        @Override
                        public void onError(Exception ex) {
                            System.err.println("Error from backend: " + ex.getMessage());
                        }
                    };

                    backendConnection.connectBlocking();

                    if (!backendConnection.isOpen()) {
                        System.err.println("Connected but socket was closed immediately. Retrying...");
                        attempt++;
                        Thread.sleep(2000);
                        continue;
                    }

                    System.out.println("Successfully connected to backend after retries");
                    return;

                } catch (Exception e) {
                    System.err.println("Failed to connect to primary at " + currentPrimaryUrl + ". Retrying...");
                    e.printStackTrace();
                    attempt++;
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {}
                }
            }

            System.err.println("Could not connect to backend after multiple attempts.");
        }).start();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client connected to coordinator: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message from client: " + message);
        if (backendConnection != null && backendConnection.isOpen()) {
            backendConnection.send(message);
        } else {
            System.err.println("Backend connection not open. Queuing message: " + message);
            messageQueue.add(message);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected from coordinator: " + conn.getRemoteSocketAddress());
        System.out.println("Connection closed. Code: " + code + ", Reason: " + reason);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Coordinator error: " + ex.getMessage());
        if (ex != null) ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("ConnectionCoordinator WebSocket server started on port " + getPort());
    }

    public void updateLeader(String newUrl) {
        System.out.println("Raw leader update received: " + newUrl);
        if (!newUrl.startsWith("ws://") && !newUrl.startsWith("wss://")) {
            System.out.println("Prepending ws:// to leader address.");
            newUrl = "ws://" + newUrl;
        }
        System.out.println("Setting currentPrimaryUrl to: " + newUrl);
        currentPrimaryUrl = newUrl;
        connectToPrimary();
    }

    public static void main(String[] args) {
        ConnectionCoordinator coordinator = new ConnectionCoordinator(new InetSocketAddress("0.0.0.0", 9999));
        coordinator.start();
        System.out.println("Coordinator running on port 9999");
    }
}
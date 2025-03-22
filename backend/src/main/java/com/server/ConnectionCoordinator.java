package com.server;

import java.net.InetSocketAddress;
import java.net.URI;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

public class ConnectionCoordinator extends WebSocketServer {

    private String currentPrimaryUrl = "ws://primary_server:8887"; // Updated on leader change
    private WebSocketClient backendConnection;

    public ConnectionCoordinator(InetSocketAddress address) {
        super(address);
        connectToPrimary();
    }

    private void connectToPrimary() {
        try {
            backendConnection = new WebSocketClient(new URI(currentPrimaryUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    System.out.println("Connected to primary at: " + currentPrimaryUrl);
                }

                @Override
                public void onMessage(String message) {
                    // Relay messages from backend to all connected clients
                    broadcast(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("Primary connection closed.");
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("Error from backend: " + ex.getMessage());
                }
            };
            backendConnection.connect();
        } catch (Exception e) {
            System.err.println("Failed to connect to primary server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client connected to coordinator: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message from client: " + message);
        if (backendConnection != null && backendConnection.isOpen()) {
            backendConnection.send(message); // Forward message to backend server
        } else {
            System.err.println("Backend connection is not open.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected from coordinator: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error in coordinator: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("ConnectionCoordinator WebSocket server started on port " + getPort());
    }

    public void updateLeader(String newUrl) {
        System.out.println("Updating leader to: " + newUrl);
        currentPrimaryUrl = newUrl;
        connectToPrimary();
    }

    public static void main(String[] args) {
        ConnectionCoordinator coordinator = new ConnectionCoordinator(new InetSocketAddress("0.0.0.0", 9999));
        coordinator.start();
        System.out.println("Coordinator running on port 9999");
    }
}
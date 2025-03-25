package com.server;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class ConnectionCoordinator extends WebSocketServer {
    private String currentPrimaryUrl = "ws://172.18.0.5:8887"; // Initial default
    private WebSocket backendConnection;

    public ConnectionCoordinator(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Client connected to coordinator: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Message from client: " + message);

        if (message.equals("GET_LEADER")) {
            System.out.println("Client requested current leader.");
            conn.send("NEW_LEADER:" + currentPrimaryUrl);
            return;
        } else if (message.startsWith("NEW_LEADER:")) {
            String newLeaderUrl = message.substring("NEW_LEADER:".length()).trim();
            System.out.println("Received NEW_LEADER message: " + newLeaderUrl);

            if (!newLeaderUrl.startsWith("ws://") && !newLeaderUrl.startsWith("wss://")) {
                newLeaderUrl = "ws://" + newLeaderUrl;
            }

            currentPrimaryUrl = newLeaderUrl;
            backendConnection = conn; // Store this connection to broadcast messages from backend
            System.out.println("Updated currentPrimaryUrl to: " + currentPrimaryUrl);
        } if (conn == backendConnection) {
            // Forward messages from the backend to all clients
            System.out.println("Message broadcasted: " + message);
            broadcast(message);
        } else {
            System.out.println("Ignoring non-backend message: " + message);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected from coordinator: " + conn.getRemoteSocketAddress());
        if (conn == backendConnection) {
            System.out.println("Backend connection lost.");
            backendConnection = null;
        }
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

    public static void main(String[] args) {
        ConnectionCoordinator coordinator = new ConnectionCoordinator(new InetSocketAddress("0.0.0.0", 9999));
        coordinator.start();
        System.out.println("Coordinator running on port 9999");
    }
}
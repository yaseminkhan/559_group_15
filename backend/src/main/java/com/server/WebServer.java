/**
 * WebServer.java
 *
 * @author Jin Song
 * @version 0.0.1
 * Simple implementation using java websocket library
 */
package com.example;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer extends WebSocketServer {

    private final Set<WebSocket> connectedClients =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public WebServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        connectedClients.add(conn);
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
        conn.send("Welcome.");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connectedClients.remove(conn);
        System.out.println("Connection closed: " + conn.getRemoteSocketAddress()
                        + " Reason: " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from " + conn.getRemoteSocketAddress()
                        + ": " + message);
        conn.send("You said: " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error from " + (conn != null ? conn.getRemoteSocketAddress() : "Server") 
                        + ": " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully on " + getPort() + ".");
    }

    @Override
    public void broadcast(String message) {
        for (WebSocket client : connectedClients) {
            client.send(message);
        }
    }

    public static void main(String[] args) {
        int port = 8887;
        WebServer server = new WebServer(
                new InetSocketAddress("localhost", port)
        );
        server.start();
        System.out.println("Web Server running on port: " + port);
    }
}

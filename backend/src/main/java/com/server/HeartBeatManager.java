package com.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeartBeatManager {
    private final String serverAddress;
    private final int heartbeatPort;
    private final List<ServerInfo> allServers;
    public static final int HEARTBEAT_TIMEOUT = 10000;

    private final String primaryServerAddress;
    private final Map<Integer, ServerInfo> knownServers = new HashMap<>();
    public final int id;

    public HeartBeatManager(String serverAddress, int heartbeatPort, List<ServerInfo> allServers, String primaryServerAddress, int serverID) {
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.primaryServerAddress = primaryServerAddress;
        this.id = serverID;

        for (ServerInfo server : allServers) {
            if (!server.getIp().equals(serverAddress)) {
                knownServers.put(server.getServerId(), server);
            }
        }
    }

    public boolean isPrimaryAlive() {
        ServerInfo primaryInfo = knownServers.values().stream()
            .filter(s -> (s.getIp() + ":" + s.getPort()).equals(primaryServerAddress))
            .findFirst()
            .orElse(null);

        if (primaryInfo == null) return false;

        long currentTime = System.currentTimeMillis();
        return (currentTime - primaryInfo.getLastHeartbeatTime()) < HEARTBEAT_TIMEOUT;
    }

    public void sendHeartbeatToAllServers() {
        for (ServerInfo server : allServers) {
            if (!server.getIp().equals(serverAddress)) {
                sendHeartbeat(server.getIp(), server.getPort());
            }
        }
    }

    public void sendHeartbeat(String serverIp, int port) {
        try (Socket socket = new Socket(serverIp, port)) {
            OutputStream output = socket.getOutputStream();
            String heartbeatMessage = "HEARTBEAT:" + id;
            output.write(heartbeatMessage.getBytes());
            System.out.println("Sent heartbeat to " + serverIp + ":" + port);
        } catch (IOException ioe) {
            System.err.println("Failed to send heartbeat to " + serverIp + ":" + port + " - " + ioe.getMessage());
        }
    }

    public void startHeartbeatListener(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Listening for heartbeats on port " + port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    InputStream input = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead = input.read(buffer);
                    if (bytesRead > 0) {
                        String message = new String(buffer, 0, bytesRead);
                        if (message.startsWith("HEARTBEAT:")) {
                            int senderId = Integer.parseInt(message.split(":")[1]);
                            ServerInfo sender = knownServers.get(senderId);
                            if (sender != null) {
                                sender.updateHeartbeatTime();
                                System.out.println("Received heartbeat from Server ID " + senderId + " (" + sender.getIp() + ")");
                            } else {
                                System.out.println("Received heartbeat from unknown Server ID: " + senderId);
                            }
                        }
                    }
                    socket.close();
                }
            } catch (IOException ioe) {
                System.err.println("Error in heartbeat listener: " + ioe.getMessage());
            }
        }).start();
    }

    public void startHeartbeatSender() {
        new Thread(() -> {
            while (true) {
                sendHeartbeatToAllServers();
                printServerStatus();
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    public void printServerStatus() {
        System.out.println("=== Server Heartbeat Status ===");
        for (ServerInfo info : knownServers.values()) {
            String status = info.isAlive() ? "ALIVE" : "DEAD";
            System.out.println("Server ID " + info.getServerId() + " (" + info.getIp() + ":" + info.getPort() + ") - " + status);
        }
        System.out.println("================================");
    }
}
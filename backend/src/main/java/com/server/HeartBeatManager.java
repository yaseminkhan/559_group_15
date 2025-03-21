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
    public static final int HEARTBEAT_TIMEOUT = 3000; // can make this longer to accomodate latency 

    private final String primaryServerAddress;
    private final Map<Integer, ServerInfo> knownServers = new HashMap<>();
    public final int id;
    private final ElectionManager electionManager;
    private final boolean isPrimary;
    private boolean hasTriggeredElection = false;

    public HeartBeatManager(String serverAddress, int heartbeatPort, List<ServerInfo> allServers, String primaryServerAddress, int serverID, boolean isPrimary) {
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.primaryServerAddress = primaryServerAddress;
        this.id = serverID;
        this.electionManager = new ElectionManager(serverAddress, serverID);
        this.isPrimary = isPrimary;

        for (ServerInfo server : allServers) {
            if (!server.getIp().equals(serverAddress)) {
                knownServers.put(server.getServerId(), server);
            }
        }
    }

    public void startPrimaryMonitor() {
        new Thread(() -> {
            while (true) {
                if (!isPrimary) {
                    System.out.println("\nCHECKING IF PRIMARY ALIVE\n");
                    isPrimaryAlive(); // triggers election if needed
                }
                try {
                    Thread.sleep(1000); // or 500ms for even quicker checks
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void isPrimaryAlive() {
        ServerInfo primaryInfo = knownServers.values().stream()
            .filter(s -> (s.getIp() + ":" + s.getPort()).equals(primaryServerAddress))
            .findFirst()
            .orElse(null);
        
        System.out.println("Last heartbeat from primary was " + (System.currentTimeMillis() - primaryInfo.getLastHeartbeatTime()) + "ms ago.");
        if (primaryInfo == null || (System.currentTimeMillis() - primaryInfo.getLastHeartbeatTime()) >= HEARTBEAT_TIMEOUT) {
            System.out.println("Primary is considered dead.");
            if (!hasTriggeredElection) {
                hasTriggeredElection = true;
                electionManager.triggerElection();
            } else {
                hasTriggeredElection = false; // Reset if primary becomes alive again
            }
        }else { 
            System.out.println("Primary is alive and well.");
        }
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
                //printServerStatus();
                // if(!isPrimary){
                //     System.out.println("\nCHECKING IF PRIMARY ALIVE\n");
                //     isPrimaryAlive(); // only backups check is primary is alive 
                // } 
                sendHeartbeatToAllServers();
                try {
                    Thread.sleep(1000);
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
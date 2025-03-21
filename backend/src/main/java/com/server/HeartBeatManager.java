package com.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeartBeatManager {
    private final String serverAddress;
    private final int heartbeatPort; //Port for heartbeat communication
    private final List<String> allServers; //List of all server addresses
    private long lastHeartbeatTime = System.currentTimeMillis(); //Timestamp of last received heartbeat
    public static final int HEARTBEAT_TIMEOUT = 10000; //Set server time out as 10 seconds
    private final Map<String, Long> lastHeartbeats = new ConcurrentHashMap<>();
    private final LeaderElectionManager leaderElectionManager;

    public HeartBeatManager(String serverAddress, int heartbeatPort, List<String> allServers, List<String> allServersElection, String heartBeatAddress, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.leaderElectionManager = new LeaderElectionManager(serverAddress, allServersElection, heartBeatAddress, this, webServer);
    }

    public void setCurrentLeader(String address) {
        leaderElectionManager.setCurrentLeader(address);
    }

    //Send heartbeats to all peer servers
    public void sendHeartbeatToAllServers() {
        for (String server : allServers) {
            if (!server.equals(serverAddress)) { //Exclude self
                String[] serverInfo = server.split(":");
                int otherServerHBPort = Integer.parseInt(serverInfo[1]);
                sendHeartbeat(serverInfo[0], otherServerHBPort);
            }
        }

    }
    
    //Send heartbeats to a specific server
    public void sendHeartbeat(String serverIp, int port) {
        // System.out.println("serverip: " + serverIp + ", port: " + port);
        try (Socket socket = new Socket(serverIp, port)) {
            OutputStream output = socket.getOutputStream(); //Create output stream to send data
            output.write("HEARTBEAT".getBytes()); //Send the heartbeat message
            System.out.println("Heartbeat sent to server: " + serverIp + ": " + port);
        } catch (IOException ioe) {
            System.err.println("Failed to send heartbeat to " + serverIp + " on port: " + port + ". This is the message: " + ioe.getMessage());
        }
    }

    //Start listening for heartbeats from other servers
    public void startHeartbeatListener(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Listening for messages on port " + port);
                while (true) {
                    Socket socket = serverSocket.accept();
                    InputStream input = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead = input.read(buffer);

                    if (bytesRead > 0) {
                        String message = new String(buffer, 0, bytesRead);
                        // Resolve hostname
                        String senderIp = socket.getInetAddress().getHostAddress();
                        String senderHost = socket.getInetAddress().getHostName();
                        String cleanHost = senderHost.split("\\.")[0]; 
                        
                        // Format in WebSocket style
                        String senderAddress = cleanHost + ":" + port;

                        // Format in WebSocket style
                        //System.out.println("Sender Address for heartbeat listener: " + senderAddress);
                        
                        if (message.equals("HEARTBEAT")) {
                            updateHeartbeat(senderAddress);
                            System.out.println("Heartbeat received from: " + senderAddress);
                        } else {
                            handleIncomingMessage(senderAddress, message);
                        }
                    }
                    socket.close();
                }
            } catch (IOException ioe) {
                System.err.println("Error in message listener: " + ioe.getMessage());
            }
        }).start();
    }

    public LeaderElectionManager getLeaderElectionManager() {
        return this.leaderElectionManager;
    }
    //Start sending heartbeats to other servers periodically
    public void startHeartbeatSender() {
        new Thread(() -> {
            while (true) {
                sendHeartbeatToAllServers();
                try {
                    Thread.sleep(5000); //Send heartbeat every 5 seconds
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    //Get timestamp of the lat received heartbeat
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void leaderStatus() {
        leaderElectionManager.checkLeaderStatus();
    }

    public boolean isServerAlive(String serverAddress) {
        long currentTime = System.currentTimeMillis();
        Long lastHeartbeat = lastHeartbeats.get(serverAddress);
        long diff = (currentTime - lastHeartbeat);
        System.out.println("Last heart beat: " + lastHeartbeat);
        System.out.println("Current time - last heart beat: " + diff);
        
        if (lastHeartbeat == null) {
            System.out.println("Server " + serverAddress + " has no recorded heartbeat.");
            return false;
        }
    
        boolean alive = (currentTime - lastHeartbeat) < HEARTBEAT_TIMEOUT;
        if (!alive) {
            System.out.println("Server " + serverAddress + " is considered dead. Removing from lastHeartbeats.");
            lastHeartbeats.remove(serverAddress); // Ensure it's not falsely marked as alive
        }
    
        System.out.println("Checking if " + serverAddress + " is alive: " + alive);
        return alive;
    }

    // Update heartbeat when received
    public void updateHeartbeat(String serverAddress) {
        lastHeartbeats.put(serverAddress, System.currentTimeMillis());
    }

    public Long getLastHeartbeat(String serverAddress) {
        return lastHeartbeats.getOrDefault(serverAddress, -1L);
    }

    public void sendMessage(String serverAddress, String message) {
        System.out.println("Server Address in sendMessage: " + serverAddress);
        String[] parts = serverAddress.split(":"); // Split by ":"
        String server = parts[0]; 
        int port = Integer.parseInt(parts[1]);
        System.out.println("Server: " + server + "port: " + port);
        try (Socket socket = new Socket(server, port);
            OutputStream output = socket.getOutputStream()) {
            output.write(message.getBytes());
            System.out.println("Sent message to " + server + " on port: " + port + " Message: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send message to " + server + ": " + e.getMessage());
        }
    }

    public void handleIncomingMessage(String senderAddress, String message) {
        if (message.equals("ELECTION")) {
            leaderElectionManager.handleElectionMessage(senderAddress);
        } else if (message.equals("BULLY")) {
            leaderElectionManager.handleBullyMessage(senderAddress);
        } else if (message.startsWith("LEADER")) {
            String newLeader = message.split(":", 2)[1];
            leaderElectionManager.handleLeaderMessage(newLeader);
        } else if (message.startsWith("NEW_LEADER:")) {
            String newLeader = message.split(":", 2)[1];
            leaderElectionManager.setCurrentLeader(newLeader);
        }

    }

}

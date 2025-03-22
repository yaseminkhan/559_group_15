package com.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HeartBeatManager {
    private final String serverAddress;
    private final int heartbeatPort; //Port for heartbeat communication
    private final List<String> allServers; //List of all server addresses
    private List<String> allHBServers; //List of all server addresses
    private long lastHeartbeatTime = System.currentTimeMillis(); //Timestamp of last received heartbeat
    public static final int HEARTBEAT_TIMEOUT = 10000; //Set server time out as 10 seconds
    private final ConcurrentHashMap<String, Long> lastHeartbeats = new ConcurrentHashMap<>();
    private final LeaderElectionManager leaderElectionManager;
    private static final Map<String, Integer> serverNameToPortMap = Map.of(
        "backup_server_1", 6001,
        "backup_server_2", 7001,
        "backup_server_3", 4001,
        "primary_server", 5001
    );

    public HeartBeatManager(String serverAddress, int heartbeatPort, List<String> allServers, List<String> allServersElection, String heartBeatAddress, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.leaderElectionManager = new LeaderElectionManager(serverAddress, allServersElection, heartBeatAddress, this, webServer);
    }

    // public void setCurrentLeader(String address) {
    //     leaderElectionManager.setCurrentLeader(address);
    // }

    //Send heartbeats to all peer servers
    public void sendHeartbeatToAllServers() throws NumberFormatException, InterruptedException {
        allHBServers = new ArrayList<>(allServers); //Create a copy to iterate through
        for (String server : allHBServers) {
            String[] serverInfo = server.split(":");
            int otherServerHBPort = Integer.parseInt(serverInfo[1]);
            sendHeartbeat(serverInfo[0], otherServerHBPort);
            
        }

    }
    
    //Send heartbeats to a specific server
    public void sendHeartbeat(String serverIp, int port) {
        System.out.println("Attempting to connect to serverip: " + serverIp + ", port: " + port);
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(serverIp, port), 1000);
            OutputStream output = socket.getOutputStream(); //Create output stream to send data
            output.write("HEARTBEAT".getBytes()); //Send the heartbeat message
            System.out.println("Heartbeat sent to server: " + serverIp + ": " + port);
            socket.close();
        } catch (SocketTimeoutException ste) {
            System.err.println("Connection to " + serverIp + " on port " + port + " timed out after 1 second.");
        }
         catch (IOException ioe) {
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
                        String senderAddress = cleanHost + ":" + serverNameToPortMap.get(cleanHost);

                        // Format in WebSocket style
                        System.out.println("Sender Address for heartbeat listener: " + senderAddress);
                        
                        if (message.equals("HEARTBEAT")) {
                            updateHeartbeat(senderAddress);
                            System.out.println("Heartbeat received from: " + senderAddress);
                        } else {
                            try {
                                handleIncomingMessage(senderAddress, message);
                            } catch (InterruptedException ex) {
                                System.err.println("Error wiht handling incoming message from :" + senderAddress);
                            }
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
                try {
                    sendHeartbeatToAllServers();
                } catch (NumberFormatException | InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(1000); //Send heartbeat every 1 second
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

    public void leaderStatus() throws InterruptedException {
        leaderElectionManager.checkLeaderStatus();
    }


    public boolean isServerAlive(String serverAddress) throws InterruptedException {
        if (serverAddress == null) {
            return true;
        }
        System.out.println("Server address: " + serverAddress);
        String cleanHost;

        if (serverAddress.contains("://")) {
            // Case 1: "ws://primary_server:8887"
            String[] parts = serverAddress.split("://"); // Split at "://"
            String[] hostParts = parts[1].split(":"); // Split at ":"
            String serverName = hostParts[0]; // Get "primary_server"
            System.out.println("Server name: " + serverName);

            cleanHost = serverName + ":" + serverNameToPortMap.get(serverName);
        } else {
            // Case 2: "primary_server:5001" (already in correct format)
            cleanHost = serverAddress;
        }
        // String cleanHost = "primary_server:7001";
        System.out.println("Clean host: " + cleanHost);
        System.out.println("Stored keys in lastHeartbeats: " + lastHeartbeats.keySet());
        long currentTime = System.currentTimeMillis();

        Long lastHeartbeat = lastHeartbeats.get(cleanHost);
        if (lastHeartbeat == null) {
            System.out.println("No heartbeat found for " + cleanHost);
            return true;
        }

        long diff = currentTime - lastHeartbeat;
        System.out.println("Current time: " + currentTime);
        System.out.println("Last heartbeat: " + lastHeartbeat);
        System.out.println("Time diff: " + diff);

        boolean alive = diff < HEARTBEAT_TIMEOUT;
        System.out.println("Alive: " + alive + " HEARTBEAT timeout: " + HEARTBEAT_TIMEOUT);
        if (!alive) {
            System.out.println("Server " + serverAddress + " is considered dead. Removing from lastHeartbeats.");
            lastHeartbeats.remove(cleanHost);
            System.out.println("Remove old primary server:  " + serverAddress);
            allServers.remove(cleanHost);
            System.out.println("Updated HB allServers: " + cleanHost);
        }

        System.out.println("Checking if " + cleanHost + " is alive: " + alive);
        return alive;
    }

    // Update heartbeat when received
    public void updateHeartbeat(String serverAddress) {
        long time = System.currentTimeMillis();
        System.out.println("Updated heart beat for server: " + serverAddress + ": " + time);
        lastHeartbeats.put(serverAddress, time);
        
        // Debugging: Print all stored heartbeats
        System.out.println("Current heartbeat map: " + lastHeartbeats);
    }

    public Long getLastHeartbeat(String serverAddress) {
        return lastHeartbeats.getOrDefault(serverAddress, -1L);
    }

    public void sendMessage(String serverAddressRecieve, String message) {
        System.out.println("Server Address in sendMessage: " + serverAddressRecieve);
        String[] parts = serverAddressRecieve.split(":"); // Split by ":"
        String server = parts[0]; 
        int port = Integer.parseInt(parts[1]);
        System.out.println("Server: " + server + "port: " + port);
        try (Socket socket = new Socket(server, port);
            OutputStream output = socket.getOutputStream()) {
            output.write(message.getBytes());
            System.out.println("Sent message to " + server + " on port: " + port + " Message: " + message);
        } catch (IOException ioe) {
            System.err.println("Failed to send message to " + server + ": " + ioe.getMessage());
        } catch (Exception e) {
            System.out.println("general exception is called, err msg " + e.getMessage());
        }
    }

    public void handleIncomingMessage(String senderAddress, String message) throws InterruptedException {
        if (message.equals("ELECTION")) {
            leaderElectionManager.handleElectionMessage(senderAddress);
        } else if (message.equals("BULLY")) {
            leaderElectionManager.handleBullyMessage(senderAddress);
        } else if (message.startsWith("LEADER")) {
            String newLeader = message.split(":", 2)[1];
            leaderElectionManager.handleLeaderMessage(newLeader);
        }

    }

}

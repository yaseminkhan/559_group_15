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
    private final List<String> allServers; //List of all server addresses
    private List<String> allHBServers; //List of all server addresses
    private long lastHeartbeatTime = System.currentTimeMillis(); //Timestamp of last received heartbeat
    public static final int HEARTBEAT_TIMEOUT = 2000; //Set server time out as 2 seconds
    private final ConcurrentHashMap<String, Long> lastHeartbeats = new ConcurrentHashMap<>();
    private final LeaderElectionManager leaderElectionManager;
    String myTailscaleIp = System.getenv("TAILSCALE_IP");

    private static final Map<String, String> properMap = Map.of(
        "8887", "5001", // primary
        "8892", "6001", // backup 1
        "8889", "7001", // backup 2
        "8890", "4001",  // backup 3
        "8891", "8001"  // backup 4
    );


    public HeartBeatManager(String serverAddress, int heartbeatPort, List<String> allServers, List<String> allServersElection, String heartBeatAddress, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.allServers = allServers;
        this.leaderElectionManager = new LeaderElectionManager(serverAddress, allServersElection, heartBeatAddress, this, webServer);
    }

    //Send heartbeats to all peer servers
    public void sendHeartbeatToAllServers() throws NumberFormatException, InterruptedException {
        allHBServers = new ArrayList<>(allServers); //Create a copy to iterate through
        for (String server : allHBServers) {
            String[] serverInfo = server.split(":");
            int otherServerHBPort = Integer.parseInt(serverInfo[1]);
            //System.out.println("sending heartbeat to server: " + serverInfo[0] + " on port: " + otherServerHBPort);
            sendHeartbeat(serverInfo[0], otherServerHBPort);
            
        }

    }
    
    //Send heartbeats to a specific server
    public void sendHeartbeat(String serverIp, int port) {
        try {
            Socket socket = new Socket();
            socket.setReuseAddress(true); // Allow address reuse
            socket.connect(new InetSocketAddress(serverIp, port), 500);
            OutputStream output = socket.getOutputStream(); //Create output stream to send data

            String[] srcAddress = serverAddress.split("://");
            String message = srcAddress[1] + ":HEARTBEAT"; // Adding tailscale IP to the heartbeat message
            output.write(message.getBytes()); //Send the heartbeat message
            socket.close();
        } catch (SocketTimeoutException ste) {
            System.err.println("Connection to " + serverIp + " on port " + port + " timed out after 0.5 seconds.");
        }
         catch (IOException ioe) {
            System.err.println("Failed to send heartbeat to server: " + serverIp + " on port: " + port + ". This is the message: " + ioe.getMessage());
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
                        // Resolve Sender Address
                        String[] parts = message.split(":");
                        // Message format : <sender_ip>:<command>:<message>
                        String senderAddress = parts[0] + ":" + parts[1];
                        // remove address from message
                        if (parts.length < 4) {
                            message = parts[2];
                        } else if (parts.length == 5) {
                            message = parts[2] + ":" + parts[3] + ":" + parts[4];
                        }
                        // HEARTBEAT or other messages
                        if (message.startsWith("HEARTBEAT")) {
                            updateHeartbeat(senderAddress);
                        } else {
                            try {
                                // Change senderAddress to hostaddressIP
                                handleIncomingMessage(senderAddress, message);
                            } catch (InterruptedException ex) {
                                System.err.println("Error with handling incoming message from: " + senderAddress);
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
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(500); //Send heartbeat every 500ms
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

        long currentTime = System.currentTimeMillis();

        Long lastHeartbeat = lastHeartbeats.get(serverAddress);
        if (lastHeartbeat == null) {
            System.out.println("No heartbeat found for " + serverAddress);
            return true;
        }

        long diff = currentTime - lastHeartbeat;

        boolean alive = diff < HEARTBEAT_TIMEOUT;
        System.out.println("Alive: " + alive + " HEARTBEAT timeout: " + HEARTBEAT_TIMEOUT);
        if (!alive) {
            System.out.println("Server " + serverAddress + " is considered dead. Removing from lastHeartbeats.");
            lastHeartbeats.remove(serverAddress);
            allServers.remove(serverAddress);
        }

        System.out.println("Checking if " + serverAddress + " is alive: " + alive);
        return alive;
    }

    // Update heartbeat when received
    public void updateHeartbeat(String serverAddress) {
        long time = System.currentTimeMillis();

        String[] parts = serverAddress.split(":"); // Split by ":"
        serverAddress = parts[0] + ":" + properMap.get(parts[1]); // Get the server address and port
        lastHeartbeats.put(serverAddress, time);
        
    }

    public Long getLastHeartbeat(String serverAddress) {
        return lastHeartbeats.getOrDefault(serverAddress, -1L);
    }

    public void sendMessage(String serverAddressRecieve, String message) {
        String[] parts = serverAddressRecieve.split(":"); // Split by ":"
        String server = parts[0]; 
        int port = Integer.parseInt(parts[1]);
        try (Socket socket = new Socket(server, port);
            OutputStream output = socket.getOutputStream()) {
            output.write(message.getBytes());
        } catch (IOException ioe) {
            System.err.println("Failed to send message to " + server + ": " + ioe.getMessage());
        } catch (Exception e) {
            System.out.println("general exception is called, err msg " + e.getMessage());
        }
    }

    public void handleIncomingMessage(String senderAddress, String message) throws InterruptedException {
        if (message.startsWith("ELECTION")) {
            System.out.println("HANDLING ELECTION!!!!");
            long electionId = Long.parseLong(message.split(":", 2)[1]);
            leaderElectionManager.handleElectionMessage(senderAddress, electionId);
        } else if (message.startsWith("BULLY")) {
            System.out.println("HANDLING BULLY!!!!");
            long electionId = Long.parseLong(message.split(":", 2)[1]);
            leaderElectionManager.handleBullyMessage(senderAddress, electionId);
        } else if (message.startsWith("LEADER")) {
            System.out.println("HANDLING LEADER!!!!");
            leaderElectionManager.handleLeaderMessage(senderAddress);
        } else if (message.startsWith("GET_LEADER")) {
            System.out.println("HANDLING GET LEADER!!!!");
            leaderElectionManager.handleGetLeaderMessage(senderAddress);
        }

    }

}

package com.server;

import java.util.List;
import java.util.Map;

public class LeaderElectionManager {
    private final String serverAddress;
    private final List<String> allServersElection;
    private final HeartBeatManager heartBeatManager;
    private final String heartBeatAddress;
    private boolean isLeader;
    private String currentLeader;
    private boolean wasBullied;
    private boolean running; // Indicates if the election process is running
    private final WebServer webServer;
    private final int timeout = 5000; // Timeout for waiting for responses
    private static final Map<String, Integer> serverNameToPortMap = Map.of(
        "backup_server_1", 6001,
        "backup_server_2", 7001,
        "backup_server_3", 4001,
        "primary_server", 5001
    );
    private static final Map<String, String> serverNameToAddressMap = Map.of(
        "backup_server_1", "ws://backup_server_1:8888",
        "backup_server_2", "ws://backup_server_2:8889",
        "backup_server_3", "ws://backup_server_3:8890",
        "primary_server", "ws://primary_server:8887"
    );


    
    public LeaderElectionManager(String serverAddress, List<String> allServersElection, String heartBeatAddress, HeartBeatManager heartBeatManager, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.allServersElection = allServersElection;
        this.heartBeatManager = heartBeatManager;
        this.isLeader = false;
        this.running = false;
        this.webServer = webServer;
        this.heartBeatAddress = heartBeatAddress;
        this.wasBullied = false;
    }


    public void initializeAsLeader() {
        System.out.println("Bootstrapping as leader: " + serverAddress);
        this.isLeader = true;
        System.out.println("leader change 4");
        this.currentLeader = serverAddress;
        running = false;
    
        // heartBeatManager.updateHeartbeat(serverAddress); // Add yourself
        heartBeatManager.startHeartbeatSender();
    
        // WebServer.serverAddressToIdMap.put(serverAddress, getServerId(serverAddress));
    
        for (String server : allServersElection) {
            if (!server.equals(heartBeatAddress)) {
                System.out.println("Leader message sent to " + server + " on initialization");
                sendLeaderMessage(server);
            }
        }
        webServer.setIsPrimary(true);
        // webServer.notifyClientsNewLeader(serverAddress);
    }

    public String getCurrentLeader() {
        return this.currentLeader;
    }

    public void checkLeaderStatus() throws InterruptedException {
        System.out.println("\nisLeader value: " + isLeader + "\n");
        if (!isLeader) {
            System.out.flush();

            System.out.println("Current leader: " + this.currentLeader);
            if (!heartBeatManager.isServerAlive(this.currentLeader) && !isLeader) { 
                System.out.println("Leader is down. Starting election...");
                initiateElection();
            }
            System.out.println("if-else block");
        }
    }

    public void initiateElection() throws InterruptedException {
        System.out.println("Initiate Election called");
        System.out.println("allServersElection:  " + allServersElection);
        System.out.println("Remove old primary heartbeat port:  " + this.currentLeader);

        String cleanHost;
        if (serverAddress.contains("://")) {
            // Case 1: "ws://primary_server:8887"
            String[] parts = this.currentLeader.split("://"); // Split at "://"
            String[] hostParts = parts[1].split(":"); // Split at ":"
            String serverName = hostParts[0]; // Get "primary_server"
            System.out.println("Server name to be removed 1: " + serverName);

            cleanHost = serverName + ":" + serverNameToPortMap.get(serverName);
        } else {
            // Case 2: "primary_server:5001" (already in correct format)
            cleanHost = serverAddress;
        }

        allServersElection.remove(cleanHost);
        System.out.println("Updated LE allServersElection:  " + allServersElection);
        
        running = true;

        // Send an election message to servers with higher ids
        for (String server : allServersElection) {
            if (server.equals(heartBeatAddress)) {
                continue; // Skip self
            }

            int otherServerId = getServerId(server);
            System.out.println("Election call to server: " + server + " It's id: " + otherServerId + ". My id: "+ getServerId(heartBeatAddress));

            // If this server has a lower ID than current server, it sends an election message to higher-id servers
            if (otherServerId > getServerId(heartBeatAddress)) {
                sendElectionMessage(server);
            }
        }

        // Wait for responses (simulate timeout)
        long startTime = System.currentTimeMillis();
        while (running && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(1000); // Sleep for a short period before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // If no response received (no higher priority server), declare self as leader
        if (running) {
            System.out.println("No response received. Declaring self as leader...");
            declareSelfAsLeader();
        } else {
            System.out.println("Election was interrupted. Another server responded.");
        }
    }

    private void sendElectionMessage(String server) throws InterruptedException {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "ELECTION");
        } else {
            System.out.println("Server " + server + " is not alive, skipping election message.");
        }
    }

    private void sendLeaderMessage(String server) {
        heartBeatManager.sendMessage(server, "LEADER:" + serverAddress);
    }

    public void handleElectionMessage(String senderAddress) throws InterruptedException {
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);

        // If the sender has a lower ID, send a bully message
        if (senderId < currentId && !this.wasBullied) {
            sendBullyMessage(senderAddress);
            running = true;
            initiateElection();
        }
    }

    private void sendBullyMessage(String server) throws InterruptedException {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "BULLY");
        }
    }

    public void handleLeaderMessage(String leaderAddress) {
        System.out.println("old leader: "+ this.currentLeader);
        this.currentLeader = leaderAddress;
        System.out.println("new leader: "+ this.currentLeader);
        // this.isLeader = false;
        // System.out.println("leader change 1");
        running = false; // Stop the election
        System.out.println("Leader elected: " + leaderAddress);
    }

    public void handleBullyMessage(String senderAddress) {
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);
        System.out.println("received bully message from: " + senderAddress);
        // If sender's id is higher, it might be the leader, so we stop the election
        if (senderId > currentId) {
            if ((this.wasBullied && (senderId > getServerId(this.currentLeader)) || !this.wasBullied)) {
                String[] hostParts = senderAddress.split(":"); // Split at ":"
                String serverName = hostParts[0]; // Get "primary_server"
                System.out.println("Server name: " + serverName);
    
                String serverAddress = serverNameToAddressMap.get(serverName);
                this.currentLeader = serverAddress;
                this.isLeader = false;
                running = false; // Stop the election process
                System.out.println("Got bullied by: " + serverAddress);
            }              
        }
        this.wasBullied = true;
    }

    private void declareSelfAsLeader() {
        this.isLeader = true;
        System.out.println("leader change 3");
        this.currentLeader = serverAddress;
        running = false;
        System.out.println("I am the new leader: " + serverAddress);
    
        // Immediately update its own heartbeat
        // heartBeatManager.updateHeartbeat(serverAddress);
        heartBeatManager.startHeartbeatSender();
    
        // Ensure the leader's ID is stored
        WebServer.serverAddressToIdMap.put(serverAddress, getServerId(serverAddress));
    
        // Notify all servers
        for (String server : allServersElection) {
            if (!server.equals(heartBeatAddress)) {                
                sendLeaderMessage(server);
            }
        }
        webServer.setIsPrimary(true);
        webServer.connectToCoordinatorAndAnnounce();
        // webServer.notifyClientsNewLeader(serverAddress);
    }

    private int getServerId(String address) {
        return WebServer.serverAddressToIdMap.getOrDefault(address, -1);
    }
}
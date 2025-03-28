package com.server;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class LeaderElectionManager {
    private final String serverAddress;
    private final List<String> allServersElection;
    private final List<String> removeServers;
    private final HeartBeatManager heartBeatManager;
    private final String heartBeatAddress;
    private boolean isLeader;
    private String currentLeader;
    private boolean wasBullied;
    private boolean running; // Indicates if the election process is running
    private long electionId = 0;
    private final WebServer webServer;
    private boolean higherId;
    private final int timeout = 1000; // Timeout for waiting for responses
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
        this.removeServers = new ArrayList<>();
    }


    public void initializeAsLeader() {
        System.out.println("Bootstrapping as leader: " + serverAddress);
        this.isLeader = true;
        this.currentLeader = serverAddress;
        this.running = false;
    
        heartBeatManager.startHeartbeatSender();
    
        for (String server : allServersElection) {
            if (!server.equals(heartBeatAddress)) {
                System.out.println("Leader message sent to " + server + " on initialization");
                sendLeaderMessage(server);
            }
        }
        
        webServer.setIsPrimary(true);
    }

    public String getCurrentLeader() {
        return this.currentLeader;
    }

    public void checkLeaderStatus() throws InterruptedException {
        System.out.println("\nisLeader value: " + isLeader + "\n");
        if (!isLeader) {
            System.out.flush();

            System.out.println(": " + this.currentLeader);
            if(this.currentLeader == null) {
                for (String server : allServersElection) {
                    if (server.equals(heartBeatAddress)) {
                        continue; // Skip self
                    }
                    heartBeatManager.sendMessage(server, "GET_LEADER");
                }
            }
            if (!heartBeatManager.isServerAlive(this.currentLeader) && !isLeader) { 
                System.out.println("Leader is down. Starting election...");
                initiateElection();
            }
        }
    }

    public void initiateElection() throws InterruptedException {
        this.higherId = false;
        electionId = System.currentTimeMillis();  // Generate a unique election ID
        System.out.println("Initiate Election called with ID: " + electionId);

        String cleanHost;
        if (serverAddress.contains("://")) {
            // Case 1: "ws://primary_server:8887"
            String[] parts = this.currentLeader.split("://"); // Split at "://"
            String[] hostParts = parts[1].split(":"); // Split at ":"
            String serverName = hostParts[0]; // Get "primary_server"
            cleanHost = serverName + ":" + serverNameToPortMap.get(serverName);
        } else {
            // Case 2: "primary_server:5001" (already in correct format)
            cleanHost = serverAddress;
        }

        allServersElection.remove(cleanHost);
        this.running = true;

        // Send an election message to servers with higher ids
        for (String server : allServersElection) {
            if (server.equals(heartBeatAddress)) {
                continue; // Skip self
            }

            int otherServerId = getServerId(server);
            System.out.println("Election call to server: " + server + " It's id: " + otherServerId + ". My id: "+ getServerId(heartBeatAddress));

            // If this server has a lower ID than current server, it sends an election message to higher-id servers
            if (otherServerId > getServerId(heartBeatAddress)) {
                // higherId = true;
                sendElectionMessage(server, electionId);
            }
        }

        allServersElection.removeAll(removeServers);

        // Wait for responses (simulate timeout)
        long startTime = System.currentTimeMillis();
        while (this.running && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(250); // Sleep for a short period before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // If no response received (no higher priority server), declare self as leader
        if (this.running && !this.higherId) {
            System.out.println("No response received. Declaring self as leader...");
            declareSelfAsLeader();
        } else {
            System.out.println("Election was interrupted. Another server responded.");
        }
    }

    private void sendElectionMessage(String server, long receivedElectionId) throws InterruptedException {
        if (heartBeatManager.isServerAlive(server)) {
            this.higherId = true;
            heartBeatManager.sendMessage(server, "ELECTION:" + receivedElectionId);
        } else {
            removeServers.add(server);
            System.out.println("Server " + server + " is not alive, skipping election message.");
        }
    }

    private void sendLeaderMessage(String server) {
        heartBeatManager.sendMessage(server, "LEADER:" + this.serverAddress);
    }

    public void handleGetLeaderMessage(String senderServerAddress) {
        if(isLeader) {
            System.out.println("Sending the leader message again to: " + senderServerAddress);
            sendLeaderMessage(senderServerAddress);
        }
    }

    public void handleElectionMessage(String senderAddress, long receivedElectionId) throws InterruptedException {
        if (receivedElectionId > this.electionId) {
            this.electionId = receivedElectionId;
            System.out.println("Ignoring outdated election message from " + senderAddress);
            return;
        }
        electionId = receivedElectionId; // Update to the latest election ID
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);

        // If the sender has a lower ID, send a bully message
        if (senderId < currentId) {
            sendBullyMessage(senderAddress, receivedElectionId);
            this.running = true;
            if (!this.wasBullied) {
                initiateElection();
            }
            
        }

    }

    private void sendBullyMessage(String server, long receivedElectionId) throws InterruptedException {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "BULLY:" + receivedElectionId);
        }
    }

    public void handleLeaderMessage(String leaderAddress) {
        this.currentLeader = leaderAddress;
        this.running = false; // Stop the election
        System.out.println("Leader elected: " + leaderAddress);

        // Demote this server to backup; maybe need this in the future. do not delete
        // webServer.setIsPrimary(false);
        // webServer.demoteToBackup(); // Start Kafka consumer
    }

    public void handleBullyMessage(String senderAddress, long receivedElectionId) {
        if (receivedElectionId > this.electionId) {
            this.electionId = receivedElectionId;
            System.out.println("Ignoring outdated bully message from " + senderAddress);
            return;
        }

        electionId = receivedElectionId;  // Update the current election ID
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);
        System.out.println("received bully message from: " + senderAddress);
        // If sender's id is higher, it might be the leader, so we stop the election
        if (senderId > currentId) {
            if ((this.wasBullied && (senderId > getServerId(this.currentLeader)) || !this.wasBullied)) {
                String[] hostParts = senderAddress.split(":"); // Split at ":"
                String serverName = hostParts[0]; // Get "primary_server"
    
                String serverAddress = serverNameToAddressMap.get(serverName);
                this.currentLeader = serverAddress;
                this.isLeader = false;
                this.running = false; // Stop the election process
                System.out.println("Got bullied by: " + serverAddress);
            }              
        }
        this.wasBullied = true;
    }

    private void declareSelfAsLeader() {
        this.isLeader = true;
        this.currentLeader = serverAddress;
        this.running = false;
        System.out.println("I am the new leader: " + serverAddress);
    
        // Immediately update its own heartbeat
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
        webServer.promoteToPrimary(); // Start Kafka producer
        webServer.connectToCoordinatorAndAnnounce();
    }

    private int getServerId(String address) {
        return WebServer.serverAddressToIdMap.getOrDefault(address, -1);
    }
}
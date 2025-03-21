package com.server;

import java.util.List;

public class LeaderElectionManager {
    private final String serverAddress;
    private final List<String> allServersElection;
    private final HeartBeatManager heartBeatManager;
    private final String heartBeatAddress;
    private boolean isLeader;
    private String currentLeader;
    private boolean running; // Indicates if the election process is running
    private final WebServer webServer;
    private final int timeout = 2000; // Timeout for waiting for responses

    public LeaderElectionManager(String serverAddress, List<String> allServersElection, String heartBeatAddress, HeartBeatManager heartBeatManager, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.allServersElection = allServersElection;
        this.heartBeatManager = heartBeatManager;
        this.isLeader = false;
        this.running = false;
        this.webServer = webServer;
        this.heartBeatAddress = heartBeatAddress;
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
                sendLeaderMessage(server);
            }
        }
    
        webServer.setIsPrimary(true);
        // webServer.notifyClientsNewLeader(serverAddress);
    }

    // public void setCurrentLeader(String address) {
    //     System.out.println("setCurrentLeader called! Setting leader to: " + address);
    //     this.currentLeader = address;
    //     System.out.println("Current leader Address: " + this.currentLeader);
    //     // Notify all other servers about the leader
    //     if (this.serverAddress.equals(this.currentLeader)) {
    //         for (String server : this.allServersElection) {
    //             if (!server.equals(this.heartBeatAddress)) {  // Avoid sending to self
    //                 System.out.println("Notifying servers about current leader");
    //                 heartBeatManager.sendMessage(server, "NEW_LEADER:" + address);
    //             }
    //         }
    //     }
    // }

    public String getCurrentLeader() {
        return this.currentLeader;
    }

    public void checkLeaderStatus() throws InterruptedException {
        System.out.println("\nisLeader value: " + isLeader + "\n");
        if (!isLeader) {
            System.out.flush();

            System.out.println("Current leader: " + this.currentLeader);
            // if (this.currentLeader == null) {
            //     Thread.sleep(30000);
            //     if (this.currentLeader == null) {
            //         initiateElection();
            //     }
            //     // initiateElection();
            // }
            // else 
            if (!heartBeatManager.isServerAlive(this.currentLeader) && !isLeader) { 
                System.out.println("Leader is down. Starting election...");
                initiateElection();
            }
            System.out.println("if-else block");
        }
    }

    public void initiateElection() throws InterruptedException {
        System.out.println("Initiate Election called");
        running = true;

        // Send an election message to servers with higher ids
        for (String server : allServersElection) {
            if (server.equals(heartBeatAddress)) {
                continue; // Skip self
            }

            int otherServerId = getServerId(server);
            System.out.println("Election call to server: " + server + " It's id: " + otherServerId);

            // If this server has a lower ID than current server, it sends an election message to higher-id servers
            if (otherServerId > getServerId(heartBeatAddress)) {
                sendElectionMessage(server);
            }
        }

        // Wait for responses (simulate timeout)
        long startTime = System.currentTimeMillis();
        while (running && (System.currentTimeMillis() - startTime) < timeout) {
            try {
                Thread.sleep(100); // Sleep for a short period before checking again
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

        // If the sender has a higher ID, send a bully message
        if (senderId > currentId) {
            sendBullyMessage(senderAddress);
        }

        // If the sender has the same or lower ID, initiate election
        if (!running) {
            initiateElection();
        }
    }

    private void sendBullyMessage(String server) throws InterruptedException {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "BULLY");
        }
    }

    public void handleLeaderMessage(String leaderAddress) {
        this.currentLeader = leaderAddress;
        // this.isLeader = false;
        // System.out.println("leader change 1");
        running = false; // Stop the election
        System.out.println("Leader elected: " + leaderAddress);
    }

    public void handleBullyMessage(String senderAddress) {
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);

        // If sender's id is higher, it might be the leader, so we stop the election
        if (senderId > currentId) {
            this.currentLeader = senderAddress;
            this.isLeader = false;
            System.out.println("leader change 2");
            running = false; // Stop the election process
            System.out.println("Leader determined: " + senderAddress);
        }
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
        webServer.notifyClientsNewLeader(serverAddress);
    }

    private int getServerId(String address) {
        return WebServer.serverAddressToIdMap.getOrDefault(address, -1);
    }
}

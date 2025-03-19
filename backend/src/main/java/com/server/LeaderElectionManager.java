package com.server;

import java.util.List;

public class LeaderElectionManager {
    private final String serverAddress;
    private final String heartBeatAddress;
    private final List<String> allServersElection;
    private final HeartBeatManager heartBeatManager;
    private boolean isLeader;
    private String currentLeader;
    private boolean receivedOkMessage;
    private final WebServer webServer;

    public LeaderElectionManager(String serverAddress, List<String> allServersElection, String heartBeatAddress, HeartBeatManager heartBeatManager, WebServer webServer) {
        this.serverAddress = serverAddress;
        this.allServersElection = allServersElection;
        this.heartBeatManager = heartBeatManager;
        // this.webServer = webServer;
        this.isLeader = false;
        this.heartBeatAddress = heartBeatAddress;
        this.receivedOkMessage = false;
        this.webServer = webServer;
    }

    public void setCurrentLeader(String address) {
        this.currentLeader = address;
        System.out.println("Current leader Address: " + this.currentLeader);
        // 🔹 Notify all other servers about the leader
        for (String server : allServersElection) {
            if (!server.equals(serverAddress)) {  // Avoid sending to self
                System.out.println("Leader message sent to: " + server);
                heartBeatManager.sendMessage(server, "NEW_LEADER:" + address);
            }
        }
    }

    public String getCurrentLeader() {
        return this.currentLeader;
    }

    public void checkLeaderStatus() {
        if (!heartBeatManager.isServerAlive(currentLeader)) {
            System.out.println("Leader is down. Starting election...");
            startElection();
        }
    }

    public void startElection() {
        receivedOkMessage = false;
        boolean higherServerExists = false;
        
        for (String server : allServersElection) {
            int otherServerId = getServerId(server);
            System.out.println("Election call to server: " + server + "It's id: " + otherServerId);
            if (otherServerId > getServerId(heartBeatAddress)) {
                System.out.println("Election message sent to: " + otherServerId);
                sendElectionMessage(server);
                higherServerExists = true;
            }
        }

        if (higherServerExists) {
            // Wait for responses (simulate a timeout)
            try {
                Thread.sleep(2000);  // Wait 2 seconds for OK messages
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // If no higher server responded, declare self as leader
        if (!receivedOkMessage) {
            declareSelfAsLeader();
        }
    }

    public void handleElectionMessage(String senderAdd) {
        receivedOkMessage = true;
        sendOkMessage(senderAdd);
    }

    private void sendElectionMessage(String server) {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "ELECTION");
        }
    }

    private void sendOkMessage(String server) {
        heartBeatManager.sendMessage(server, "OK");
    }

    public void declareSelfAsLeader() {
        this.isLeader = true;
        this.currentLeader = serverAddress;
        webServer.setIsPrimary(true);
        for (String server : allServersElection) {
            heartBeatManager.sendMessage(server, "NEW_LEADER:" + serverAddress);
        }
        System.out.println("New leader elected: " + serverAddress);
    }

    public void handleNewLeaderMessage(String leaderAddress) {
        this.isLeader = false;
        this.currentLeader = leaderAddress;
        System.out.println("New leader acknowledged: " + leaderAddress);
    }

    private int getServerId(String address) {
        return WebServer.serverAddressToIdMap.getOrDefault(address, -1);
    }
}

package com.server;

public class ElectionManager {
    private final String serverAddress;
    private final int serverId;

    public ElectionManager(String serverAddress, int serverId) {
        this.serverAddress = serverAddress;
        this.serverId = serverId;
    }

    public void triggerElection() {
        System.out.println("=== ELECTION TRIGGERED ===");
        System.out.println("Server " + serverAddress + " (ID: " + serverId + ") is initiating an election.");
        // Later, you can add logic to determine the new primary, notify others, etc.
    }
}
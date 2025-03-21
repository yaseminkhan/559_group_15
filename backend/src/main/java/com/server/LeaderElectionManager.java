// package com.server;

// import java.util.List;

// public class LeaderElectionManager {
//     private final String serverAddress;
//     private final String heartBeatAddress;
//     private final List<String> allServersElection;
//     private final HeartBeatManager heartBeatManager;
//     private boolean isLeader;
//     private String currentLeader;
//     private boolean receivedOkMessage;
//     private final WebServer webServer;

//     public LeaderElectionManager(String serverAddress, List<String> allServersElection, String heartBeatAddress, HeartBeatManager heartBeatManager, WebServer webServer) {
//         this.serverAddress = serverAddress;
//         this.allServersElection = allServersElection;
//         this.heartBeatManager = heartBeatManager;
//         // this.webServer = webServer;
//         this.isLeader = false;
//         this.heartBeatAddress = heartBeatAddress;
//         this.receivedOkMessage = false;
//         this.webServer = webServer;
//     }

//     public void setCurrentLeader(String address) {
//         this.currentLeader = address;
//         System.out.println("Current leader Address: " + this.currentLeader);
//         // 🔹 Notify all other servers about the leader
//         for (String server : allServersElection) {
//             if (!server.equals(serverAddress)) {  // Avoid sending to self
//                 System.out.println("Leader message sent to: " + server);
//                 heartBeatManager.sendMessage(server, "NEW_LEADER:" + address);
//             }
//         }
//     }

//     public String getCurrentLeader() {
//         return this.currentLeader;
//     }

//     public void checkLeaderStatus() {
//         if (!heartBeatManager.isServerAlive(currentLeader)) {
//             System.out.println("Leader is down. Starting election...");
//             startElection();
//         }
//     }

//     // public void startElection() {
//     //     receivedOkMessage = false;
//     //     boolean higherServerExists = false;
        
//     //     for (String server : allServersElection) {
//     //         int otherServerId = getServerId(server);
//     //         System.out.println("Election call to server: " + server + "It's id: " + otherServerId);
//     //         if (otherServerId > getServerId(heartBeatAddress)) {
//     //             System.out.println("Election message sent to: " + otherServerId);
//     //             sendElectionMessage(server);
//     //             higherServerExists = true;
//     //         }
//     //     }

//     //     if (higherServerExists) {
//     //         // Wait for responses (simulate a timeout)
//     //         try {
//     //             Thread.sleep(2000);  // Wait 2 second for OK messages
//     //         } catch (InterruptedException e) {
//     //             Thread.currentThread().interrupt();
//     //         }
//     //     }

//     //     // If no higher server responded, declare self as leader
//     //     if (!receivedOkMessage) {
//     //         declareSelfAsLeader();
//     //     }
//     // }

//     // public void startElection() {
//     //     receivedOkMessage = false;
//     //     boolean higherServerExists = false;
        
//     //     // Track the highest priority (server with highest id)
//     //     String highestPriorityServer = null;
        
//     //     for (String server : allServersElection) {
//     //         int otherServerId = getServerId(server);
//     //         System.out.println("Election call to server: " + server + " It's id: " + otherServerId);
            
//     //         // Compare with the current server's ID to see if this server has a higher priority
//     //         if (otherServerId > getServerId(heartBeatAddress)) {
//     //             System.out.println("Election message sent to: " + otherServerId);
//     //             sendElectionMessage(server);
//     //             higherServerExists = true;
//     //             // Keep track of the highest server id encountered
//     //             if (highestPriorityServer == null || otherServerId > getServerId(highestPriorityServer)) {
//     //                 highestPriorityServer = server;
//     //             }
//     //         }
//     //     }

//     //     if (higherServerExists) {
//     //         // Wait for responses (simulate a timeout)
//     //         try {
//     //             Thread.sleep(2000);  // Wait for 2 seconds for OK messages
//     //         } catch (InterruptedException e) {
//     //             Thread.currentThread().interrupt();
//     //         }
//     //     }

//     //     // If no higher server responded or no "OK" message was received, declare self as leader
//     //     if (!receivedOkMessage && highestPriorityServer == null) {
//     //         declareSelfAsLeader();
//     //     } else {
//     //         // If the highest priority server was determined, wait for it to become leader
//     //         if (highestPriorityServer != null && !highestPriorityServer.equals(serverAddress)) {
//     //             System.out.println("Waiting for server " + highestPriorityServer + " to become leader...");
//     //         }
//     //     }
//     // }

//     public void startElection() {
//         receivedOkMessage = false;
//         boolean higherServerExists = false;
        
//         // Track the highest priority (server with highest id)
//         String highestPriorityServer = null;

//         // Ensure the leader is checked and updated
//         if (heartBeatManager.isServerAlive(currentLeader)) {
//             // Current leader is alive, no need for election
//             System.out.println("Current leader is alive, no election needed.");
//             return;
//         }

//         System.out.println("Leader is down. Starting election...");

//         // Proceed with election among backup servers
//         for (String server : allServersElection) {
//             // Skip the current server from election
//             if (server.equals(serverAddress)) {
//                 continue;
//             }

//             int otherServerId = getServerId(server);
//             System.out.println("Election call to server: " + server + " It's id: " + otherServerId);
            
//             // Compare with the current server's ID to see if this server has a higher priority
//             if (otherServerId > getServerId(heartBeatAddress)) {
//                 System.out.println("Election message sent to: " + otherServerId);
//                 sendElectionMessage(server);
//                 higherServerExists = true;
                
//                 // Keep track of the highest server id encountered
//                 if (highestPriorityServer == null || otherServerId > getServerId(highestPriorityServer)) {
//                     highestPriorityServer = server;
//                 }
//             }
//         }

//         // Wait for responses (simulate a timeout)
//         if (higherServerExists) {
//             long startTime = System.currentTimeMillis();
//             while (!receivedOkMessage && (System.currentTimeMillis() - startTime) < 2000) { // Wait 2 seconds for OK messages
//                 try {
//                     Thread.sleep(100);  // Sleep for a bit before checking again
//                 } catch (InterruptedException e) {
//                     Thread.currentThread().interrupt();
//                 }
//             }
//         }

//         // If no higher server responded, declare self as leader
//         if (!receivedOkMessage) {
//             declareSelfAsLeader();
//         } else {
//             // If the highest priority server was determined, wait for it to become leader
//             if (highestPriorityServer != null && !highestPriorityServer.equals(serverAddress)) {
//                 System.out.println("Waiting for server " + highestPriorityServer + " to become leader...");
//             }
//         }
//     }


//     public void handleElectionMessage(String senderAdd) {
//         receivedOkMessage = true;
//         sendOkMessage(senderAdd);
//     }

//     // private void sendElectionMessage(String server) {
//     //     if (heartBeatManager.isServerAlive(server)) {
//     //         heartBeatManager.sendMessage(server, "ELECTION");
//     //     }
//     // }
//     private void sendElectionMessage(String server) {
//         System.out.println("Sending ELECTION message to: " + server); // Add logging for debugging
//         if (heartBeatManager.isServerAlive(server)) {
//             heartBeatManager.sendMessage(server, "ELECTION");
//         } else {
//             System.out.println("Server " + server + " is not alive, skipping election message.");
//         }
//     }


//     // private void sendOkMessage(String server) {
//     //     heartBeatManager.sendMessage(server, "OK");
//     // }
//     private void sendOkMessage(String server) {
//         System.out.println("Sending OK message to: " + server); // Add logging for debugging
//         heartBeatManager.sendMessage(server, "OK");
//     }

//     public void declareSelfAsLeader() {
//         this.isLeader = true;
//         this.currentLeader = serverAddress;
//         webServer.setIsPrimary(true);
//         for (String server : allServersElection) {
//             heartBeatManager.sendMessage(server, "NEW_LEADER:" + serverAddress);
//         }
//         System.out.println("New leader elected: " + serverAddress);
//     }

//     public void handleNewLeaderMessage(String leaderAddress) {
//         this.isLeader = false;
//         this.currentLeader = leaderAddress;
//         System.out.println("New leader acknowledged: " + leaderAddress);

//         // Notify clients to reconnect to the new leader
//         webServer.notifyClientsNewLeader(leaderAddress);
//     }

//     private int getServerId(String address) {
//         return WebServer.serverAddressToIdMap.getOrDefault(address, -1);
//     }
// }
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

    public void setCurrentLeader(String address) {
        this.currentLeader = address;
        this.isLeader = true;
        System.out.println("Current leader Address: " + this.currentLeader);
        // Notify all other servers about the leader
        if (this.serverAddress.equals(this.currentLeader)) {
            for (String server : this.allServersElection) {
                if (!server.equals(this.heartBeatAddress)) {  // Avoid sending to self
                    System.out.println("Notifying servers about current leader");
                    heartBeatManager.sendMessage(server, "NEW_LEADER:" + address);
                }
            }
        }
        // for (String server : this.allServersElection) {
        //     if (!server.equals(this.heartBeatAddress)) {  // Avoid sending to self
        //         System.out.println("Notifying servers about current leader");
        //         heartBeatManager.sendMessage(server, "NEW_LEADER:" + address);
        //     }
        // }
    }

    public String getCurrentLeader() {
        return this.currentLeader;
    }

    public void checkLeaderStatus() {
        System.out.println("Current leader: " + this.currentLeader);
        if (this.currentLeader == null) {
            initiateElection();
        }
        else if (!heartBeatManager.isServerAlive(this.currentLeader) && !isLeader) { 
            System.out.println("Leader is down. Starting election...");
            initiateElection();
        }
        System.out.println("if-else block");
        // if (!heartBeatManager.isServerAlive(currentLeader)) {
        //     System.out.println("Leader is down. Starting election...");
        //     initiateElection();
        // }
    }

    public void initiateElection() {
        running = true;

        // Track the highest priority (server with highest id)
        String highestPriorityServer = null;

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
            declareSelfAsLeader();
        }
    }

    private void sendElectionMessage(String server) {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "ELECTION");
        } else {
            System.out.println("Server " + server + " is not alive, skipping election message.");
        }
    }

    private void sendLeaderMessage(String server) {
        heartBeatManager.sendMessage(server, "LEADER:" + serverAddress);
    }

    public void handleElectionMessage(String senderAddress) {
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

    private void sendBullyMessage(String server) {
        if (heartBeatManager.isServerAlive(server)) {
            heartBeatManager.sendMessage(server, "BULLY");
        }
    }

    public void handleLeaderMessage(String leaderAddress) {
        this.currentLeader = leaderAddress;
        this.isLeader = false;
        running = false; // Stop the election
        System.out.println("Leader elected: " + leaderAddress);

        // Notify clients to reconnect to the new leader
        webServer.notifyClientsNewLeader(leaderAddress);
    }

    public void handleBullyMessage(String senderAddress) {
        int senderId = getServerId(senderAddress);
        int currentId = getServerId(heartBeatAddress);

        // If sender's id is higher, it might be the leader, so we stop the election
        if (senderId > currentId) {
            this.currentLeader = senderAddress;
            this.isLeader = false;
            running = false; // Stop the election process
            System.out.println("Leader determined: " + senderAddress);
        }
    }

    // private void declareSelfAsLeader() {
    //     this.isLeader = true;
    //     this.currentLeader = serverAddress;
    //     System.out.println("I am the new leader: " + serverAddress);

    //     // Notify all servers that this server is the leader
    //     for (String server : allServersElection) {
    //         if (!server.equals(heartBeatAddress)) {
    //             sendLeaderMessage(server);
    //         }
    //     }

    //     // Notify the web server that this is the leader
    //     webServer.setIsPrimary(true);
    // }
    private void declareSelfAsLeader() {
        this.isLeader = true;
        this.currentLeader = serverAddress;
        running = false;
        System.out.println("I am the new leader: " + serverAddress);
    
        // Immediately update its own heartbeat
        heartBeatManager.updateHeartbeat(serverAddress);
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

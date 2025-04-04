package com.server;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

public class WebServer extends WebSocketServer {

    //Map to store connected users and their Websocket connections
    private final ConcurrentHashMap<WebSocket, User> connectedUsers = new ConcurrentHashMap<>();
    //Map to store active games by game code
    private final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();
    //Map to store temporarily disconnected users
    private final ConcurrentHashMap<String, User> temporarilyDisconnectedUsers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Chat>> chatUpdate = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<Game.CanvasUpdate>> gameCanvasUpdate = new ConcurrentHashMap<>();
    private final Set<WebSocket> pendingConnections = ConcurrentHashMap.newKeySet();


    private final HeartBeatManager heartBeatManager; //HeartbeatManager instance
    private final ReplicationManager replicationManager; //ReplicationManager instance
    private boolean isPrimary; //Flag to indicate if this server is the primary server
    private String heartBeatAddress;
    private final String myServerAddress;

    private String coordinatorAddress;
    private WebSocketClient coordinatorConnection;

    public static final Map<Integer, String> serverIdToAddressMap = new HashMap<>();
    public static final Map<String, Integer> serverAddressToIdMap = new HashMap<>();

    public WebServer(InetSocketAddress address, boolean isPrimary, String serverAddress, int heartbeatPort,
            List<String> allServers, List<String> allServersElection, String currentServer) {
        super(address);
        this.myServerAddress = serverAddress;
        this.isPrimary = isPrimary;
        this.heartBeatAddress = currentServer;
        this.coordinatorAddress = "ws://" + System.getenv("COORDINATOR_IP") + ":9999";

        this.heartBeatManager = new HeartBeatManager(serverAddress, heartbeatPort, allServers, allServersElection,
                heartBeatAddress, this); //Initialize the HeartbeatManager
        this.replicationManager = new ReplicationManager(this, isPrimary, serverAddress, heartbeatPort, allServers,

            activeGames, connectedUsers, temporarilyDisconnectedUsers, chatUpdate, gameCanvasUpdate);
        System.out.println("DEBUG: DOES PRIMARY : " + allServers.isEmpty());
        if (!allServers.isEmpty()) {
            this.heartBeatManager.startHeartbeatListener(heartbeatPort);
            this.heartBeatManager.startHeartbeatSender();
        }

        if (isPrimary) {
            heartBeatManager.getLeaderElectionManager().initializeAsLeader();
            connectToCoordinatorAndAnnounce();
        }


        //Set timer to periodically send the full game state to backups
        if (isPrimary) {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    replicationManager.sendFullGameState();
                    System.out.println("Sent full game state");
                }
            }, 0, 5000); //Send full game every 5 seconds


            // Start sending incremental updates 
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    for (String gameCode : activeGames.keySet()) {
                        replicationManager.sendIncrementalUpdate(gameCode);
                    }
                }
            }, 0, 200); // Send full game every 200 mili-seconds
        }

        new Thread(() -> {
            while (true) {
                //System.out.println("Check leader status");
                try {
                    heartBeatManager.leaderStatus();
                    Thread.sleep(1000); //check leader status every second
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    public void promoteToPrimary() {
        this.isPrimary = true;
        replicationManager.switchToPrimary();
        connectToCoordinatorAndAnnounce();
        // Start sending full game state periodically
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                replicationManager.sendFullGameState();
            }
        }, 0, 5000); // Send full game every 5 seconds

        // Start sending incremental updates 
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                for (String gameCode : activeGames.keySet()) {
                    replicationManager.sendIncrementalUpdate(gameCode);
                }
            }
        }, 0, 200); // Send full game every 200 mili-seconds
    }

    public void demoteToBackup() {
        this.isPrimary = false;
        replicationManager.switchToBackup();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("\n========== onOpen() CALLED ==========");
        System.out.println("Socket opened from: " + conn.getRemoteSocketAddress());

        pendingConnections.add(conn); // track as unauthenticated
    }

    public void handleReconnect(WebSocket conn, String userId) {
        System.out.println("\n========== HANDLE RECONNECT ==========");
        System.out.println("Attempting to reconnect user: " + userId);

        // Prevent reconnecting an already-connected user
        for (User existing : connectedUsers.values()) {
            if (existing != null && userId.equals(existing.getId())) {
                System.out.println("User already connected. Ignoring duplicate reconnect.");
                return;
            }
        }

        User user = null;

        // Check temporarily disconnected users
        if (temporarilyDisconnectedUsers.containsKey(userId)) {
            user = temporarilyDisconnectedUsers.remove(userId);
            System.out.println("Reconnected from temporarily disconnected list: " + user.getUsername());
        }

        // If still null, it's a brand new user
        if (user == null) {
            user = new User("Guest_" + conn.getRemoteSocketAddress().getPort());
            System.out.println("Created new user: " + user.getUsername());
        }

        // Bind the socket to the user
        connectedUsers.put(conn, user);
        pendingConnections.remove(conn); // remove from pending

        // Send user ID so frontend can store it if needed
        conn.send("USER_ID:" + user.getId());
        System.out.println("Finalized connection for user: " + user.getUsername() + " (" + user.getId() + ")");

        // If user was in a game, re-add to the game and update player list
        if (user.getGameCode() != null && activeGames.containsKey(user.getGameCode())) {
            Game game = activeGames.get(user.getGameCode());

            if (!game.hasPlayer(user)) {
                game.addPlayer(user);
                System.out.println("Re-added user to game: " + game.getGameCode());
            }

            broadcastGamePlayers(game);
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //Handle WebSocket disconnections
        pendingConnections.remove(conn);
        User removedUser = connectedUsers.remove(conn);

        if (removedUser != null) {
            System.out.println(
                    "User temporarily disconnected: " + removedUser.getUsername() + " (" + removedUser.getId() + ")");

            // Move user to temporarily disconnected list
            temporarilyDisconnectedUsers.put(removedUser.getId(), removedUser);

            // Get the game the user was in
            String gameCode = removedUser.getGameCode();
            if (gameCode != null) {
                Game game = activeGames.get(gameCode);

                if (game != null && game.getDrawer() != null && game.getDrawer().equals(removedUser)) {

                    // Schedule a delayed check before removing or replacing them
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            game.removePlayer(removedUser);
                            broadcastGamePlayers(game);
                            System.out.println("User permanently removed from game: " + removedUser.getUsername());

                            game.resetForRound();
                            // Notify players that the drawer has disconnected
                            broadcastToGame(game, "DRAWER_DISCONNECTED");
                            if (temporarilyDisconnectedUsers.containsKey(removedUser.getId())) {
                                temporarilyDisconnectedUsers.remove(removedUser.getId());

                                game.cancelTimer();

                                // Drawer did not reconnect, so select a new drawer            
                                if (game.getPlayers().size() >= 2) {
                                    System.out.println("Starting new round...");
                                    startNewRound(game);
                                } else {
                                    broadcastToGame(game, "GAME_OVER");
                                }
                            }
                        }
                    }, 5000);

                } else {
                    // If the disconnected user was NOT the drawer, just remove them after timeout
                    new Timer().schedule(new TimerTask() {
                        @Override
                        public void run() {
                            if (temporarilyDisconnectedUsers.containsKey(removedUser.getId())) {
                                game.removePlayer(removedUser);
                                broadcastGamePlayers(game);
                                if (game.getPlayers().size() < 2) {
                                    broadcastToGame(game, "GAME_OVER");
                                }
                                System.out.println("User permanently removed from game: " + removedUser.getUsername());
                                temporarilyDisconnectedUsers.remove(removedUser.getId());
                            }
                        }
                    }, 5000);
                }
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        //System.out.println("Received message from " + conn.getRemoteSocketAddress() + ": " + message);

        //Send data to backups whenever a message is received from the client
        if (!isPrimary)
            return;

        if (message.startsWith("/reconnect ")) {
            String userId = message.substring(11).trim();
            handleReconnect(conn, userId);
        } else if (message.startsWith("/setname ")) {
            handleSetUsername(conn, message.substring(9).trim());
        } else if (message.equals("/creategame")) {
            handleCreateGame(conn);
        } else if (message.startsWith("/getgame ")) {
            String gameCode = message.substring(9).trim();
            handleGetGame(conn, gameCode);
        } else if (message.startsWith("/join-game ")) {
            String gameCode = message.substring(11).trim();
            handleJoinGame(conn, gameCode);
        } else if (message.startsWith("/word-selection ")) {
            String gameCode = message.substring(15).trim();
            handleGetWords(conn, gameCode);
        } else if (message.startsWith("/startgame ")) {
            String gameCode = message.split(" ")[1];
            String userId = message.split(" ")[2];
            handleStartGame(conn, gameCode, userId);
        } else if (message.startsWith("/select-word ")) {
            String[] parts = message.split(" ");
            if (parts.length < 3) {
                conn.send("ERROR: Invalid word selection format.");
                return;
            }
            String gameCode = parts[1];
            String selectedWord = parts[2];

            handleWordSelection(conn, gameCode, selectedWord);
        } else if (message.startsWith("/drawer-joined ")) {
            String gameCode = message.substring(15).trim();
            handleDrawerJoined(conn, gameCode);
        } else if (message.startsWith("/round-over ")) {
            String gameCode = message.substring(12).trim();
            Game game = activeGames.get(gameCode);
            if (game != null) {
                startNewRound(game);
            }
        } else if (message.startsWith("/endgame ")) {
            String gameCode = message.split(" ")[1];
            handleEndGame(conn, gameCode);
        } else if (message.startsWith("/chat-history ")) {
            String[] parts = message.split(" ", 2);
            String gameCode = parts[1];
            handleChatRequest(conn, gameCode);
        } else if (message.startsWith("/chat ")) {
            String[] parts = message.split(" ", 3);
            String gameCode = parts[1];
            String chatData = parts[2];

            var game = activeGames.get(gameCode);
            if (game == null) {
                System.err.println("ERROR: Game not found.");
                return;
            }
            handleChat(conn, gameCode, chatData);
        } else if (message.startsWith("/canvas-update ")) {
            String[] parts = message.split(" ", 3);

            if (parts.length < 3) {
                conn.send("ERROR: Invalid canvas update format.");
                return;
            }

            String gameCode = parts[1];
            String json = parts[2];

            if (activeGames.get(gameCode) == null) {
                System.err.println("ERROR: Game not found."); // Code repetition.
                return;
            }

            handleCanvasUpdate(conn, gameCode, json);

        } else if (message.startsWith("/clear-canvas")) {
            String json = message.substring("/clear-canvas ".length());

            try {
                Gson gson = new Gson();
                CanvasClear clearEvent = gson.fromJson(json, CanvasClear.class);
                String gameCode = clearEvent.getGameCode();
                Game game = activeGames.get(gameCode);

                if (game != null) {
                    int updatedClock = game.getLogicalClock().getAndUpdate(clearEvent.getSequenceNumber());
                    clearEvent.setSequenceNumber(updatedClock);

                    System.out.println("[LAMPORT][CLEAR] Sender: " + clearEvent.getId() +
                        ", Frontend TS: " + clearEvent.getSequenceNumber() +
                        ", Backend TS: " + updatedClock);

                    game.addEvent(clearEvent);
                    broadcastToGame(game, "CANVAS_CLEAR");
                }

            } catch (Exception e) {
                System.out.println("ERROR: Invalid clear canvas format.");
                e.printStackTrace();
            }
        } else if (message.startsWith("/getcanvas")) {
            String[] parts = message.split(" ");
            if (parts.length < 2) {
                System.out.println("ERROR: Invalid canvas history request format.");
                return;
            }
            String gameCode = parts[1];
            int lastIndex = Integer.parseInt(parts[2]);
            handleGetCanvasHistory(conn, gameCode, lastIndex);
        } else if (message.startsWith("NEW_LEADER:")) {
            String newLeaderAddress = message.split(":")[1].trim();
            System.out.println("Received new leader update: " + newLeaderAddress);

            // Notify the connected client to reconnect
            conn.send("RECONNECT_TO_NEW_LEADER:" + newLeaderAddress);

            // Optionally, close the current connection to force reconnection
            conn.close();
        } else {
            conn.send("Unknown command.");
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        //Handle WebSocket errors
        System.err.println(
                "Error from " + (conn != null ? conn.getRemoteSocketAddress() : "Server") + ": " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        //Notify when the WebSocket server starts successfully
        System.out.println("WebSocket server started successfully on " + getPort() + ".");
    }

    public void notifyClientsNewLeader(String newLeaderAddress) {
        System.out.println("Notifying coordinator about new leader: " + newLeaderAddress);

        if (coordinatorConnection != null && coordinatorConnection.isOpen()) {
            coordinatorConnection.send("NEW_LEADER:" + newLeaderAddress);
        } else {
            System.err.println("Coordinator connection not open!");
        }
    }

    public static String getServerAddressFromId(int serverId) {
        return serverIdToAddressMap.getOrDefault(serverId, "Unknown");
    }

    public static int getServerIdFromAddress(String serverAddress) {
        return serverAddressToIdMap.getOrDefault(serverAddress, -1);
    }

    public void setIsPrimary(boolean val) {
        System.out.println("Is Primary: " + val);
        this.isPrimary = val;
    }

    public static void main(String[] args) {
        //Run as primary server if no port is indicated
        if (args.length < 3) {
            System.err.println(
                    "Usage: java com.server.WebServer <port> <heart-beatport> <other-servers> <primary boolean>");
            System.exit(1);
        }
        
        Map<String, Integer> serverNameToIdMap = Map.of(
            System.getenv("BACKUP_SERVER_1_IP") + ":6001", 1,
            System.getenv("BACKUP_SERVER_2_IP") + ":7001", 2,
            System.getenv("BACKUP_SERVER_3_IP") + ":4001", 3,
            System.getenv("BACKUP_SERVER_4_IP") + ":8001", 4,
            System.getenv("PRIMARY_SERVER_IP") + ":5001", 5
        );

        int port = Integer.parseInt(args[0]); //First server port
        int heartbeatPort = Integer.parseInt(args[1]); //First heartbeat port
        List<String> allServersElection = new ArrayList<>(Arrays.asList(args[2].split(","))); //Other servers
        List<String> allServers = new ArrayList<>(Arrays.asList(args[2].split(","))); //Other server
        boolean isPrimary = Boolean.parseBoolean(args[3]); //Obtain boolean for which is primary
        
        String serverName = System.getenv("TAILSCALE_IP"); // This gets the Docker container name
        // Define a mapping from hostnames to integer server IDs

        String currentServer = serverName + ":" + heartbeatPort;
        System.out.println("Current Server: " + currentServer);
        System.out.println("Server Name to ID Map: " + serverNameToIdMap);
        System.out.println("Looking for current server: " + currentServer);
        int serverId = serverNameToIdMap.getOrDefault(currentServer, -1); // Assign ID or default (-1 if unknown)
        if (serverId == -1) {
            System.err.println("Unknown hostname: " + serverName);
            System.exit(1);
        }
        // Construct a unique server address
        String serverAddress = "ws://" + serverName + ":" + port;
        serverIdToAddressMap.put(serverId, currentServer);
        serverAddressToIdMap.put(currentServer, serverId);

        allServersElection.add(currentServer);

        // Map all servers
        for (String server : allServersElection) {
            int id = serverNameToIdMap.getOrDefault(server, -1);
            if (id != -1) {
                serverIdToAddressMap.put(id, server);
                serverAddressToIdMap.put(server, id);
            }
        }

        System.out.println("Server Name: " + serverName);
        System.out.println("Server ID: " + serverId);
        System.out.println("isPrimary: " + isPrimary);
        System.out.println("serverAddress: " + serverAddress);
        System.out.println("heartbeatPort: " + heartbeatPort);
        System.out.println("allServers: " + allServers);
        System.out.println("All Servers for leader election: " + allServersElection);

        //Create and start WebSocket server
        WebServer server = new WebServer(new InetSocketAddress("0.0.0.0", port), isPrimary, serverAddress,
                heartbeatPort, allServers, allServersElection, currentServer);
        server.start();
        System.out.println("isPrimary: " + args[3]);
        System.out.println("Web Server running on port: " + port);
        System.out.println("Heartbeat listener running on port: " + heartbeatPort);
    }

    public void connectToCoordinatorAndAnnounce() {
        // ===== DEBUG PRINTS =====
        System.out.println("\n===== DEBUG: Starting connectToCoordinatorAndAnnounce =====");
        System.out.println("Connected users by id from replication manager:");
        for (Map.Entry<String, User> entry : replicationManager.getConnectedUsersById().entrySet()) {
            User user = entry.getValue();
            System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
        }

        System.out.println("Connected Users:");
        for (Map.Entry<WebSocket, User> entry : connectedUsers.entrySet()) {
            User user = entry.getValue();
            System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
        }

        System.out.println("\nActive Games:");
        for (Map.Entry<String, Game> entry : activeGames.entrySet()) {
            System.out.println(" - Game Code: " + entry.getKey());
            Game game = entry.getValue();
            for (User player : game.getPlayers()) {
                System.out.println("   * Player: " + player.getUsername() + " (ID: " + player.getId() + ")");
            }
        }

        System.out.println("\nTemporarily Disconnected Users:");
        for (User user : temporarilyDisconnectedUsers.values()) {
            System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
        System.out.println("===== END DEBUG =====\n");

        temporarilyDisconnectedUsers.putAll(replicationManager.getConnectedUsersById());
        replicationManager.stopKafkaConsumer();

        System.out.println("\n--- RESUMING ANY INCOMPLETE ROUNDS ---");
        for (Map.Entry<String, Game> entry : activeGames.entrySet()) {
            Game game = entry.getValue();
            System.out.println("Checking game: " + entry.getKey());
            System.out.println("  - wordToDraw: " + game.getWordToDraw());
            System.out.println("  - Time Left: " + game.getTimeLeft());
            System.out.println("  - Timer Exists: " + (game.getTimer() != null));
            System.out.println("  - gameStarted: " + game.hasGameStarted());
            System.out.println("  - gameEnded: " + game.isGameEnded());
            System.out.println("  - drawer: " + (game.getDrawer() != null ? game.getDrawer().getUsername() : "null"));
        }

        System.out.println("Promoted to primary. Checking if any game rounds need to be resumed...");
        for (Game game : activeGames.values()) {
            if (game.isRoundInProgress()) {
                System.out.println("Resuming round for game: " + game.getGameCode());
                startRoundTimer(game);
            } else {
                System.out.println("No active round to resume for game: " + game.getGameCode());
            }
        }

        new Thread(() -> {
            while (true) {
                try {
                    if (coordinatorConnection == null || coordinatorConnection.isClosed()) {
                        System.out.println("Attempting to connect to coordinator...");
                        System.out.println("Before reinit: COORD" + coordinatorAddress);
                        coordinatorAddress = "ws://" + System.getenv("COORDINATOR_IP") + ":9999";
                        System.out.println("After reinit: COORD" + coordinatorAddress);

                        System.out.println("Attempting WebSocket to: " + coordinatorAddress);
                        coordinatorConnection = new WebSocketClient(new URI(coordinatorAddress)) {
                            @Override
                            public void onOpen(ServerHandshake handshake) {
                                System.out.println("Connected to coordinator.");
                                send("NEW_LEADER:" + myServerAddress);
                            }

                            @Override
                            public void onMessage(String message) {
                            }

                            @Override
                            public void onClose(int code, String reason, boolean remote) {
                                System.out.println("Coordinator connection closed. Will retry...");
                            }

                            @Override
                            public void onError(Exception ex) {
                                System.err.println("Coordinator WebSocket error: " + ex.getMessage());
                            }
                        };

                        coordinatorConnection.connectBlocking();
                    }

                    // Sleep and then check again
                    Thread.sleep(5000);

                } catch (Exception e) {
                    System.err.println("Failed to connect to coordinator: " + e.getMessage());
                    try {
                        Thread.sleep(5000); // wait before retrying
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }).start();
    }

    // ======================================================== Game Logic Methods ========================================================

    public void handleChatRequest(WebSocket conn, String gameCode) {
        Game game = activeGames.get(gameCode);
        Gson gson = new Gson();
        List<Chat> chat = game.getChatEvents();
        conn.send("HISTORY: " + gson.toJson(chat));
        conn.send("LAMPORT: " + game.getLogicalClock().getTime());
    }
    
    public void handleChat(WebSocket conn, String gameCode, String chatData) {
        Game game = activeGames.get(gameCode);

        if (game == null) {
            System.err.println("ERROR: Game not found.");
            return;
        }


        synchronized (game) {
            Gson gson = new Gson();
            // DEBUG: Raw JSON string from frontend
            System.out.println("[LAMPORT DEBUG] Raw chat JSON: " + chatData);
        
            Chat chat = gson.fromJson(chatData, Chat.class);
        
            // DEBUG: Confirm deserialization
            System.out.println("[LAMPORT DEBUG] Deserialized Chat object: " + gson.toJson(chat));
        
            User user = connectedUsers.get(conn);
            if (user == null) {
                System.out.println("ERROR: User not found for connection.");
                conn.send("ERROR: You are not connected.");
                return;
            }
        
            // Lamport timestamp logic
            int frontendTime = chat.getSequenceNumber();
            int updatedTime = game.getLogicalClock().getAndUpdate(frontendTime);
            chat.setSequenceNumber(updatedTime); // apply server-finalized timestamp
        
            System.out.println(String.format(
                "[LAMPORT][CHAT] User ID: %s | Frontend TS: %d | Assigned Backend TS: %d",
                chat.getId(),
                frontendTime,
                updatedTime
            ));
            System.out.println("[LAMPORT] Backend clock now: " + game.getLogicalClock().getTime());
        
            chat = game.addMessage(chat); 
            System.out.println("Handle Chat Request: " + game.getChatEvents());
            // Update chatUpdate
            System.out.println("Chat update map updated");
            synchronized (chatUpdate) {
                chatUpdate.computeIfAbsent(gameCode, k -> new ArrayList<>()).add(chat);
            }
            broadcastToGame(game, "/chat " + gameCode + " " + gson.toJson(chat));
        }
    }

    public void handleStartGame(WebSocket conn, String gameCode, String userId) {
        Game game = activeGames.get(gameCode);
        if (game != null) {
            List<User> players = game.getPlayers();

            if (players.isEmpty()) {
                conn.send("ERROR: No players in game.");
                return;
            }

            // Reset all players scores to 0
            game.resetScores();
            game.setGameStarted(true);
            // System.out.println("  - wordToDraw: " + game.getWordToDraw());
            // System.out.println("  - Time Left: " + game.getTimeLeft());
            // System.out.println("  - Timer Exists: " + (game.getTimer() != null));
            // System.out.println("  - gameStarted: " + game.hasGameStarted());
            // System.out.println("  - gameEnded: " + game.isGameEnded());
            // System.out.println("  - drawer: " + (game.getDrawer() != null ? game.getDrawer().getUsername() : "null"));
            
            // Assign a drawer 
            System.out.println("Assign next drawer called\n");
            game.assignNextDrawer();
            System.out.println("first drawer called\n");
            User firstDrawer = game.getDrawer();

            String startGameMessage = "GAME_STARTED: " + firstDrawer.getId();
            broadcastToGame(game, startGameMessage);
            System.out.println(startGameMessage);
            System.out.println(
                    "Game started for game code: " + gameCode + ". First drawer is: " + firstDrawer.getUsername());
        }
    }

    public void handleEndGame(WebSocket conn, String gameCode) {
        Game game = activeGames.get(gameCode);
        if (game != null) {
            User user = connectedUsers.get(conn);
            if (user != null) {
                game.confirmEndGame(user.getId());
                System.out.println("User " + user.getUsername() + " confirmed end game for " + gameCode);

                // Check if all players have confirmed
                if (game.getPlayers().size() == game.sizeOfPlayersConfirmedEnd()) {
                    // for (User player: game.getPlayers()) {
                    //     player = new User("Guest_" + conn.getRemoteSocketAddress().getPort());
                    // }

                    game.clearGame();
                    activeGames.remove(gameCode);
                    // connectedUsers.clear(); // causing issues
                    temporarilyDisconnectedUsers.clear();
                    System.out.println("Game " + gameCode + " has ended and been removed.");
                    //broadcastToGame(game, "GAME_ENDED");

                    // Send a special message to Kafka to indicate the game has ended
                    // if (isPrimary) {
                    //     replicationManager.sendIncrementalUpdate("/game-ended " + gameCode);
                    // }
                }
            }
        }
    }

    public void handleDrawerJoined(WebSocket conn, String gameCode) {

        if (!isPrimary)
            return;

        Game game = activeGames.get(gameCode);
        if (game != null) {
            // Notify all players that the drawer has joined
            broadcastToGame(game, "DRAWER_JOINED: " + gameCode);
            System.out.println("Drawer has joined game: " + gameCode + ". Timer should start now.");
        }
    }

    public void startNewRound(Game game) {

        if (!isPrimary)
            return;

        if (game == null)
            return;


        game.resetForRound(); // Reset round state
        chatUpdate.clear();
        gameCanvasUpdate.clear();

        if (!game.hasAvailableDrawer()) {
            broadcastToGame(game, "GAME_OVER");
            System.out.println("All rounds complete. Waiting for players to exit.");
            game.endGame();
            return;
        }

        game.nextTurn();
        game.setRoundStarted(false);

        // Notify all players about the new round and new drawer
        broadcastToGame(game, "NEW_ROUND: " + game.getCurrentRound() + " DRAWER: " + game.getDrawer().getId());
        broadcastToGame(game, "CANVAS_CLEAR");
    }

    /*
     * game timer handled by server 
     */
    public void startRoundTimer(Game game) {

        if (!isPrimary)
            return;

        if (game == null)
            return;

        // Cancel the existing timer if it exists
        game.cancelTimer();

        if (game.getTimeLeft() <= 0 || game.getTimeLeft() > 60) {
            game.setTimeLeft(60); // Only reset if invalid or uninitialized
        }

        // Ensure only one timer runs per game
        Timer roundTimer = new Timer();
        game.setTimer(roundTimer);

        roundTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int timeLeft = game.getTimeLeft();

                // Stop timer if all players guessed or timer runs out.
                if (timeLeft <= 0 || game.allPlayersGuessed()) {
                    roundTimer.cancel(); // Stop timer
                    broadcastToGame(game, "ROUND_OVER");
                    startNewRound(game); // Move to next round
                    return;
                }

                game.setTimeLeft(timeLeft - 1);
                broadcastToGame(game, "TIMER_UPDATE: " + game.getTimeLeft());
            }
        }, 0, 1000); // Run every second
    }

    public void handleGetGame(WebSocket conn, String gameCode) {
        //System.out.println("Fetching game data for code: " + gameCode);
        Game game = activeGames.get(gameCode);

        if (game == null) {
            conn.send("ERROR: Game not found.");
            return;
        }

        String playersJson = game.getPlayersJson();
        conn.send(new Gson().toJson(Map.of("type", "GAME_PLAYERS", "data", playersJson)));
    }

    public void handleJoinGame(WebSocket conn, String gameCode) {
        Game game = activeGames.get(gameCode);
        System.out.println("Retrieving game: " + gameCode);
        System.out.println("Available games: " + activeGames.keySet());

        if (game == null) {
            System.out.println("ERROR: Game not found.");
            conn.send("ERROR: Game not found.");
            return;
        }

        User user = connectedUsers.get(conn);
        if (user == null) {
            System.out.println("ERROR: User not found.");
            conn.send("ERROR: User not found.");
            return;
        }

        // Remove user from any previous game before joining the new one
        String previousGameCode = user.getGameCode();
        if (previousGameCode != null && !previousGameCode.equals(gameCode)) {
            Game previousGame = activeGames.get(previousGameCode);
            if (previousGame != null) {
                previousGame.removePlayer(user);
                broadcastGamePlayers(previousGame);
                System.out.println("Removed user " + user.getUsername() + " from previous game: " + previousGameCode);
            }
        }

        if (game.isFull()) {
            System.out.println("ERROR: Game is full. Cannot add " + user.getUsername());
            conn.send("ERROR: Game is full.");
            return;
        }

        game.addPlayer(user);
        user.setGameCode(gameCode); // Store the new game code

        System.out.println("User " + user.getUsername() + " joined game: " + gameCode);
        broadcastGamePlayers(game);

        conn.send("JOIN_SUCCESS:" + gameCode);
    }

    public void broadcastToGame(Game game, String message) {
        if (game != null) {
            // System.out.println("game:" + game);
            for (User player : game.getPlayers()) {
                // System.out.println("players user:" + player);
                // System.out.println("Connected Users:");
                // for (Map.Entry<WebSocket, User> entry : connectedUsers.entrySet()) {
                //     User user = entry.getValue();
                //     System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
                // }
                WebSocket conn = getConnectionByUser(player);
                if (conn != null) {
                    conn.send(message);
                    if (!message.startsWith("TIMER_UPDATE")) {
                        //System.out.println("Broadcast to: " + player.getUsername() + " Message: " + message);
                    }
                } else {
                    if (isPrimary) {
                        System.out.println("Could not find connection for " + player.getUsername());
                    }
                }
            }
        }
    }

    public void broadcastGamePlayers(Game game) {
        String playersJson = game.getPlayersJson();
        String message = new Gson().toJson(Map.of("type", "GAME_PLAYERS", "data", playersJson));

        System.out.println("\nBroadcasting updated player list for game: " + game.getGameCode());
        System.out.println("Players in game:");
        for (User player : game.getPlayers()) {
            System.out.println(" - " + player.getUsername() + " (ID: " + player.getId() + ")");
        }

        for (User player : game.getPlayers()) {
            WebSocket conn = getConnectionByUser(player);

            if ((conn != null)) {
                conn.send(message);
                System.out.println("Sent player list to: " + player.getUsername());
            } else {
                if (isPrimary) {
                    System.out.println("Could not find connection for " + player.getUsername());
                }
            }
        }
    }

    public WebSocket getConnectionByUser(User user) {
        return connectedUsers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(user))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public void handleCreateGame(WebSocket conn) {
        User user = connectedUsers.get(conn);
        if (user == null) {
            conn.send("ERROR: You must be connected first.");
            return;
        }

        // Check if a game already exists
        if (!activeGames.isEmpty()) {
            conn.send("ERROR: A game is already created.");
            return;
        }

        // Check if the user is in an existing game and remove them
        String previousGameCode = user.getGameCode();
        if (previousGameCode != null) {
            Game previousGame = activeGames.get(previousGameCode);
            if (previousGame != null) {
                previousGame.removePlayer(user);
                broadcastGamePlayers(previousGame);
                System.out.println("Removed user " + user.getUsername() + " from previous game: " + previousGameCode);
            }
        }

        // Generate a new game code
        String gameCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Game newGame = new Game(gameCode);
        newGame.addPlayer(user);
        activeGames.put(gameCode, newGame);

        user.setGameCode(gameCode); // Store the gameCode in the user object

        conn.send("GAME_CREATED:" + gameCode);
        broadcastGamePlayers(newGame);

        System.out.println("New game created: " + gameCode + " by " + user.getUsername());
    }

    public void handleSetUsername(WebSocket conn, String newUsername) {
        if (newUsername.isEmpty()) {
            conn.send("ERROR: Invalid username. Try again.");
            return;
        }

        User user = connectedUsers.get(conn);
        if (user == null) {
            conn.send("ERROR: User not found.");
            return;
        }

        user.setUsername(newUsername);
        conn.send("USERNAME_SET:" + newUsername);
        System.out.println("User set name: " + newUsername);
    }

    public void handleGetWords(WebSocket conn, String gameCode) {
        List<String> randomWords = Words.getRandomWordChoices();
        String jsonResponse = new Gson().toJson(Map.of("type", "WORDS", "data", randomWords));
        System.out.println("\n========== Sending Words ==========");
        System.out.println("Game Code: " + gameCode);
        System.out.println("Generated Words: " + randomWords);
        System.out.println("JSON Sent: " + jsonResponse);
        conn.send(jsonResponse);
    }

    public void handleWordSelection(WebSocket conn, String gameCode, String word) {

        if (!isPrimary)
            return;

        Game game = activeGames.get(gameCode);
        if (game != null) {
            game.setCurrentWord(word);

            // Notify all players that the word has been selected
            String message = "WORD_SELECTED: " + word;
            broadcastToGame(game, message);
            System.out.println("Word selected: " + word + ". Starting round...");
            game.setRoundStarted(true);
        }

        // Reset timer 
        game.setTimeLeft(60);

        // Start the timer for the new round
        startRoundTimer(game);
    }

    public void handleCanvasUpdate(WebSocket conn, String gameCode, String json) {
        Game game = activeGames.get(gameCode);
        if (game == null) {
            System.out.println("ERROR: Game Not Found: " + gameCode);
            return;
        }


        synchronized (game) {
            try {
                Gson gson = new Gson();
                Game.CanvasUpdate update = gson.fromJson(json, Game.CanvasUpdate.class);

                int frontendTS = update.getSequenceNumber();
                int newSeq = game.getLogicalClock().getAndUpdate(frontendTS);
                update.setSequenceNumber(newSeq);

                System.out.println(String.format(
                    "[LAMPORT][CANVAS] User ID: %s | Frontend TS: %d | Assigned Backend TS: %d",
                    update.getId(),
                    frontendTS,
                    newSeq
                ));
                System.out.println("[LAMPORT] Backend clock now: " + game.getLogicalClock().getTime());
                
                game.addCanvasUpdate(update);

                // Update gameCanvasUpdate
                System.out.println("Canvas update map updated.");
                synchronized (gameCanvasUpdate) {
                    gameCanvasUpdate.computeIfAbsent(gameCode, k -> new ArrayList<>()).add(update);
                }

            } catch (Exception e) {
                System.out.println("ERROR: Invalid canvas update format.");
            }
        }
    }

    public void handleGetCanvasHistory(WebSocket conn, String gameCode, int lastIndex) {
        Game game = activeGames.get(gameCode);
        if (game == null) {
            System.out.println("ERROR: Game Not Found: " + gameCode);
            return;
        }

        List<Game.CanvasUpdate> entireList = game.getCanvasEvents();

        if (lastIndex < 0) {
            lastIndex = 0;
        }

        if (lastIndex > entireList.size()) {
            lastIndex = entireList.size();
        }

        List<Game.CanvasUpdate> newStrokes = entireList.subList(lastIndex, entireList.size());
        Gson gson = new Gson();
        String newStrokesJson = gson.toJson(newStrokes);
        int newLastIndex = lastIndex + newStrokes.size();

        //System.out.println("Canvas History Requested: " + gameCode + " Last Index: " + lastIndex);
        conn.send("CANVAS_HISTORY " + newLastIndex + " " + newStrokesJson);
    }

    public ConcurrentHashMap<WebSocket, User> getConnectedUsers() {
        return connectedUsers;
    }
}
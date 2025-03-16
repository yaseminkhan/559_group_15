package com.server;

import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer extends WebSocketServer {

    //Map to store connected users and their Websocket connections
    private final ConcurrentHashMap<WebSocket, User> connectedUsers = new ConcurrentHashMap<>();
    //Map to store active games by game code
    private final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();
    //Map to store temporarily disconnected users
    private final ConcurrentHashMap<String, User> temporarilyDisconnectedUsers = new ConcurrentHashMap<>();
    //Map to store the output streams
    // private final Map<String, OutputStream> backupOutputs = new ConcurrentHashMap<>();

    private final HeartBeatManager heartBeatManager; //HeartbeatManager instance
    private final ReplicationManager replicationManager; //ReplicationManager instance
    private boolean isPrimary; //Flag to indicate if this server is the primary server

    public WebServer(InetSocketAddress address, boolean isPrimary, String serverAddress, int heartbeatPort, List<String> allServers) {
        super(address);
        this.isPrimary = isPrimary;
        
        this.heartBeatManager = new HeartBeatManager(serverAddress, heartbeatPort, allServers); //Initialize the HeartbeatManager
        // Initialize the ReplicationManager with a reference to this WebServer instance
        this.replicationManager = new ReplicationManager(this, isPrimary, serverAddress, heartbeatPort, allServers,
                activeGames, connectedUsers, temporarilyDisconnectedUsers);
        if (!allServers.isEmpty()) {
            this.heartBeatManager.startHeartbeatListener(heartbeatPort);
            this.heartBeatManager.startHeartbeatSender();
        }
        
        //Set timer to periodically send the full game state to backups
        if (isPrimary) {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    replicationManager.sendFullGameState();
                }
            }, 0, 10000); //Send full game every 10 seconds
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String existingUserId = handshake.getFieldValue("User-ID"); //Handle new WebSocket connections
        User user;

        //Check if user is reconnecting
        if (existingUserId != null && temporarilyDisconnectedUsers.containsKey(existingUserId)) {
            user = temporarilyDisconnectedUsers.remove(existingUserId);
            connectedUsers.put(conn, user);
            System.out.println("Reconnected user: " + user.getUsername());
        } else {
            //Create new user for the connection
            user = new User("Guest_" + conn.getRemoteSocketAddress().getPort());
            connectedUsers.put(conn, user);
        }

        conn.send("USER_ID:" + user.getId()); //Send user's ID back to the client
        System.out.println("User connected: " + user.getUsername() + " (" + user.getId() + ")");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        //Handle WebSocket disconnections
        User removedUser = connectedUsers.remove(conn);

        if (removedUser != null) {
            System.out.println(
                    "User temporarily disconnected: " + removedUser.getUsername() + " (" + removedUser.getId() + ")");

            // Move user to temporarily disconnected list
            temporarilyDisconnectedUsers.put(removedUser.getId(), removedUser);

            // Print users for debugging
            System.out.println("Current connected users:");
            for (User user : connectedUsers.values()) {
                System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
            }

            System.out.println("Temporarily disconnected users:");
            for (User user : temporarilyDisconnectedUsers.values()) {
                System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
            }
            
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
                                if(game.getPlayers().size() >= 2){
                                    System.out.println("Starting new round...");
                                    startNewRound(game);
                                } else{
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
                                if(game.getPlayers().size() < 2){
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
        if (isPrimary) {
            replicationManager.sendIncrementalUpdate(message);
        }

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
            handleChat(conn, gameCode, chatData);
        } else if (message.startsWith("/canvas-update ")) {
            //System.out.println("Canvas Update From: " + conn.getRemoteSocketAddress() + ": " + message);
            // /canvas-update <gameCode> <json>
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                conn.send("ERROR: Invalid canvas update format.");
                return;
            }
            String gameCode = parts[1];
            String json = parts[2];
            handleCanvasUpdate(conn, gameCode, json);

        } else if (message.startsWith("/clear-canvas")) {
            String gameCode = message.split(" ")[1];
            Game game = activeGames.get(gameCode);
            if (game != null) {
                game.clearCanvasHistory();
                broadcastToGame(game, "CANVAS_CLEAR");
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

    public static void main(String[] args) {
        //Run as primary server if no port is indicated
        if (args.length < 3) {
            System.err.println("Usage: java com.server.WebServer <port> <heart-beatport> <other servers> <primary boolean>");
            System.exit(1);
        }
        
        int port = Integer.parseInt(args[0]); //First server port
        int heartbeatPort = Integer.parseInt(args[1]); //First heartbeat port
        List<String> allServers = List.of(args[2].split(",")); //Other server
        String serverAddress = "ws://primary_server: " + port; // This server's address
        boolean isPrimary = Boolean.parseBoolean(args[3]); //Obtain boolean for which is primary
        
        
        System.out.println("isPrimary" + isPrimary);
        System.out.println("serverAddress" + serverAddress);
        System.out.println("heartbeatPort" + heartbeatPort);
        System.out.println("allServers" + allServers);
        
        //Create and start WebSocket server
        WebServer server = new WebServer(new InetSocketAddress("primary_server", port), isPrimary, serverAddress, heartbeatPort, allServers);
        server.start();
        System.out.println("isPrimary: " + args[3]);
        System.out.println("Web Server running on port: " + port);
        System.out.println("Heartbeat listener running on port: " + heartbeatPort);
    }


    // ======================================================== Game Logic Methods ========================================================

    public void handleChatRequest(WebSocket conn, String gameCode) {
        var game = activeGames.get(gameCode);
        var gson = new Gson();
        var chat = game.getChatMessages();
        // System.out.println(chat);
        conn.send("HISTORY: " + gson.toJson(chat));
    }

    public void handleChat(WebSocket conn, String gameCode, String chatData) {
        var game = activeGames.get(gameCode);
        var gson = new Gson();
        var chat = gson.fromJson(chatData, Chat.class);
        var user = connectedUsers.get(conn);
        //DEBUGGING ONLY
        if (user == null) {
            System.out.println("ERROR: User not found for connection.");
            conn.send("ERROR: You are not connected.");
            return;
        }
        //DEBUGGING END
        chat.sender = user.getUsername();
        chat = game.addMessage(chat); // Make sure to clear data after /new-round.
        broadcastToGame(game, "/chat " + gameCode + " " + gson.toJson(chat));
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
                if (connectedUsers.size() == game.sizeOfPlayersConfirmedEnd()) {
                    game.clearGame();
                    activeGames.remove(gameCode);
                    connectedUsers.clear();
                    temporarilyDisconnectedUsers.clear();
                    System.out.println("Game " + gameCode + " has ended and been removed.");
                    //broadcastToGame(game, "GAME_ENDED");
                }
            }
        }
    }

    public void handleDrawerJoined(WebSocket conn, String gameCode) {
        Game game = activeGames.get(gameCode);
        if (game != null) {
            // Notify all players that the drawer has joined
            broadcastToGame(game, "DRAWER_JOINED: " + gameCode);
            System.out.println("Drawer has joined game: " + gameCode + ". Timer should start now.");
        }
    }

    public void startNewRound(Game game) {
        if (game == null) return;

        game.resetForRound();  // Reset round state

        if (!game.hasAvailableDrawer()) { 
            broadcastToGame(game, "GAME_OVER");
            System.out.println("All rounds complete. Waiting for players to exit.");
            game.endGame();
            return;
        }

        game.nextTurn();
        
        // Notify all players about the new round and new drawer
        broadcastToGame(game, "NEW_ROUND: " + game.getCurrentRound() + " DRAWER: " + game.getDrawer().getId());
        broadcastToGame(game, "CANVAS_CLEAR");
    }

    /*
     * game timer handled by server 
     */
    public void startRoundTimer(Game game) {
        if (game == null)
            return;

        // Cancel the existing timer if it exists
        game.cancelTimer();

        game.setTimeLeft(60); // Reset timer for new round

        game.clearCanvasHistory();

        // game.setTimeLeft(5); // short timer for testing

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

    public void handleReconnect(WebSocket conn, String userId) {
        System.out.println("\n========== HANDLE RECONNECT ==========");
        System.out.println("Attempting to reconnect user: " + userId);

        // Check if user is already connected
        for (Map.Entry<WebSocket, User> entry : connectedUsers.entrySet()) {
            if (entry.getValue().getId().equals(userId)) {
                System.out.println("User is already connected. Ignoring duplicate reconnect.");
                return;
            }
        }

        // Check if user is in temporarilyDisconnectedUsers
        if (temporarilyDisconnectedUsers.containsKey(userId)) {
            User existingUser = temporarilyDisconnectedUsers.remove(userId);
            connectedUsers.put(conn, existingUser);
            System.out.println("User reconnected: " + existingUser.getUsername() + " (" + userId + ")");

            // Restore game if they were in one
            if (existingUser.getGameCode() != null && activeGames.containsKey(existingUser.getGameCode())) {
                Game game = activeGames.get(existingUser.getGameCode());

                // Ensure user is added back to the game
                if (!game.hasPlayer(existingUser)) {
                    game.addPlayer(existingUser);
                    System.out.println("Re-added " + existingUser.getUsername() + " to game: " + game.getGameCode());
                }

                System.out.println("User " + existingUser.getUsername() + " was in game: " + game.getGameCode());

                // Instead of sending "RECONNECTED", immediately send updated player list
                broadcastGamePlayers(game);
                return;
            }

            System.out.println("User was not in a game.");
            return;
        }

        System.out.println("ERROR: User ID not found, unable to reconnect.");
        conn.send("ERROR: User ID not found.");

        // Debugging logs
        System.out.println("Current connected users:");
        for (User user : connectedUsers.values()) {
            System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
        System.out.println("Temporarily disconnected users:");
        for (User user : temporarilyDisconnectedUsers.values()) {
            System.out.println(" - " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
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
            for (User player : game.getPlayers()) {
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
                if (isPrimary){
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
        Game game = activeGames.get(gameCode);
        if (game != null) {
            game.setCurrentWord(word);

            // Notify all players that the word has been selected
            String message = "WORD_SELECTED: " + word;
            broadcastToGame(game, message);
            System.out.println("Word selected: " + word + ". Starting round...");
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

        try {
            Gson gson = new Gson();
            Game.CanvasUpdate update = gson.fromJson(json, Game.CanvasUpdate.class);
            game.addCanvasUpdate(update);
        } catch (Exception e) {
            System.out.println("ERROR: Invalid canvas update format.");
        }
    }

    public void handleGetCanvasHistory(WebSocket conn, String gameCode, int lastIndex) {
        Game game = activeGames.get(gameCode);
        if (game == null) {
            System.out.println("ERROR: Game Not Found: " + gameCode);
            return;
        }

        List<Game.CanvasUpdate> entireList = game.getCanvasHistory();

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
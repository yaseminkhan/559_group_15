package com.server;

import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WebServer extends WebSocketServer {

    // Stores WebSocket -> User object
    private final ConcurrentHashMap<WebSocket, User> connectedUsers = new ConcurrentHashMap<>();

    // Stores active games (gameCode -> Game instance)
    private final ConcurrentHashMap<String, Game> activeGames = new ConcurrentHashMap<>();

    public WebServer(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Create a new user with a default name (temporary, will be updated later)
        User newUser = new User("Guest_" + conn.getRemoteSocketAddress().getPort());

        // Store the user
        connectedUsers.put(conn, newUser);

        // Send the user their ID (frontend will need this)
        conn.send("USER_ID:" + newUser.getId());

        System.out.println("New user connected: " + newUser.getUsername() + " (" + newUser.getId() + ")");
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        User removedUser = connectedUsers.remove(conn);
        if (removedUser != null) {
            System.out.println("User disconnected: " + removedUser.getUsername() + " (" + removedUser.getId() + ")");
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from " + conn.getRemoteSocketAddress() + ": " + message);

        if (message.startsWith("/setname ")) {
            handleSetUsername(conn, message.substring(9).trim());
        } else if (message.equals("/creategame")) {
            handleCreateGame(conn);
        } else if (message.startsWith("/getgame ")) {
            String gameCode = message.substring(9).trim();
            handleGetGame(conn, gameCode);
        } else {
            conn.send("Unknown command.");
        }
    }

    /*
    * Handles retrieving game information.
    */
    private void handleGetGame(WebSocket conn, String gameCode) {
        Game game = activeGames.get(gameCode);
        
        if (game == null) {
            conn.send("ERROR: Game not found.");
            return;
        }

        String playersJson = game.getPlayersJson();
        conn.send(new Gson().toJson(Map.of("type", "GAME_PLAYERS", "data", playersJson)));
    }

    /*
     * Handles creating a new game and adding the user to it.
     */
    private void handleCreateGame(WebSocket conn) {
        User user = connectedUsers.get(conn);
    
        if (user == null) {
            conn.send("Error: You must be connected first.");
            return;
        }
    
        // Generate a new 6-character game code
        String gameCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    
        // Create new game with a specified game code
        Game newGame = new Game(gameCode);
        newGame.addPlayer(user);
    
        // Store the game
        activeGames.put(gameCode, newGame);
    
        // Send game code back to user
        conn.send("GAME_CREATED:" + gameCode);
    
        System.out.println("Game created: " + gameCode + " by " + user.getUsername());
    }

    private void handleSetUsername(WebSocket conn, String newUsername) {
        if (newUsername.isEmpty()) {
            conn.send("ERROR: Invalid username. Try again.");
            return;
        }
    
        // Check if the user exists
        User user = connectedUsers.get(conn);
        if (user == null) {
            conn.send("ERROR: User not found.");
            return;
        }
    
        // Update username
        user.setUsername(newUsername);
        conn.send("USERNAME_SET:" + newUsername);
    
        System.out.println("User set name: " + newUsername);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Error from " + (conn != null ? conn.getRemoteSocketAddress() : "Server") + ": " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully on " + getPort() + ".");
    }

    public static void main(String[] args) {
        int port = 8887;
        WebServer server = new WebServer(new InetSocketAddress("localhost", port));
        server.start();
        System.out.println("Web Server running on port: " + port);
    }
}
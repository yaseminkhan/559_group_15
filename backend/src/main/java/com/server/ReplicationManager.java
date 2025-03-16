package com.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.java_websocket.WebSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class ReplicationManager {

    private final WebServer webServer;
    private final boolean isPrimary;
    private final String serverAddress;
    private final List<String> allServers;
    private final int heartbeatPort;
    private final ConcurrentHashMap<String, Game> activeGames;
    private final ConcurrentHashMap<String, User> connectedUsersById;
    private final ConcurrentHashMap<String, User> temporarilyDisconnectedUsers;
    private OutputStream backupIncrOutput;
    private OutputStream backupFullGameOutput;
    private KafkaProducer<String, String> kafkaProducer;
    private KafkaConsumer<String, String> kafkaConsumer;

    public ReplicationManager(WebServer webServer, boolean isPrimary, String serverAddress, int heartbeatPort, List<String> allServers,
                             ConcurrentHashMap<String, Game> activeGames,
                             ConcurrentHashMap<WebSocket, User> connectedUsers,
                             ConcurrentHashMap<String, User> temporarilyDisconnectedUsers) {
        this.webServer = webServer;
        this.isPrimary = isPrimary;
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.activeGames = activeGames;
        this.connectedUsersById = new ConcurrentHashMap<>(); //Wrapper map since ReplicationManager does not have a websocket
        this.temporarilyDisconnectedUsers = temporarilyDisconnectedUsers;

        for (Map.Entry<WebSocket, User> entry : connectedUsers.entrySet()) {
            this.connectedUsersById.put(entry.getValue().getId(), entry.getValue());
        }

        if (isPrimary) {
            // Initialize Kafka producer for primary server
            Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            kafkaProducer = new KafkaProducer<>(producerProps);
        } else {
            
            Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
            consumerProps.put(ConsumerConfig. GROUP_ID_CONFIG, "game-state-consumer-group");
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            kafkaConsumer = new KafkaConsumer<>(consumerProps);
            kafkaConsumer.subscribe(Arrays.asList("game-state", "incremental-updates"));

            //Start thread to consume messages from Kafka
            new Thread(() -> {
                while (true) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(1000)); // Increased timeout
                    for (ConsumerRecord<String, String> record : records) {
                        if (record.topic().equals("game-state")) {
                            // Handle game-state messages (JSON)
                            updateGameState(record.value());
                        } else if (record.topic().equals("incremental-updates")) {
                            // Handle incremental-updates messages (plain string)
                            processIncrementalUpdate(record.value());
                        }
                    }
                }
            }).start();
        }

        if (isPrimary) {
            connectToBackups();
        }
        else {
            startReplicationListeners();
        }
    }

    // Connect to backup servers (primary server only)
    private void connectToBackups() {
        for (String server : allServers) {
            if (!server.equals(serverAddress)) { // Exclude self
                String[] serverInfo = server.split(":");
                int otherServerHBPort = Integer.parseInt(serverInfo[1]);
                String serverIp = serverInfo[0];
                try {
                    Socket incrSocket = new Socket(serverIp, otherServerHBPort + 1);
                    backupIncrOutput = incrSocket.getOutputStream(); // Create output stream to send data
                    System.out.println("Connected to backups: " + serverIp + ": " + (otherServerHBPort + 1));
                } catch (IOException ioe) {
                    System.err.println("Failed to connect to backups: " + ioe.getMessage());
                }
                try {
                    Socket fullGameSocket = new Socket(serverIp, otherServerHBPort + 2);
                    backupFullGameOutput = fullGameSocket.getOutputStream(); // Create output stream to send data
                    System.out.println("Connected to backups: " + serverIp + ": " + (otherServerHBPort + 2));
                } catch (IOException ioe) {
                    System.err.println("Failed to connect to backups: " + ioe.getMessage());
                }
            }
        }
    }

    // Start listeners for replication data (secondary servers only)
    private void startReplicationListeners() {
        // Listen for incremental updates
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(heartbeatPort + 1)) {
                System.out.println("Listening for incremental updates on port " + (heartbeatPort + 1));
                Socket socket = serverSocket.accept();
                while (true) {
                    System.out.println("socket open for listening to incremental: on port " + (heartbeatPort + 1));
                    socket.setKeepAlive(true);
                    InputStream input = socket.getInputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead = input.read(buffer);
                    if (bytesRead > 0) {
                        String message = new String(buffer, 0, bytesRead);
                        System.out.println("received incremental update: " + buffer);
                        processIncrementalUpdate(message);
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in incremental update listener: " + e.getMessage());
            }
        }).start();

        // Listen for full game state syncs
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(heartbeatPort + 2)) {
                System.out.println("Listening for full game state syncs on port " + (heartbeatPort + 2));
                Socket socket = serverSocket.accept();
                while (true) {
                    System.out.println("socket open for listening to full game: on port " + (heartbeatPort + 2));
                        socket.setKeepAlive(true);
                        InputStream input = socket.getInputStream();
                        byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer
                        int bytesRead = input.read(buffer);
                        System.out.println("READING DATA YAAY");
                        if (bytesRead > 0) {
                            String gameStateJson = new String(buffer, 0, bytesRead);
                            System.out.println("what did I read?");
                            System.out.println("bytesRead: " + buffer);
                            System.out.println("gameStateJson: " + gameStateJson);
                            updateGameState(gameStateJson);
                        }
                        System.out.println("FINISHED READNIG");
                }
            } catch (IOException e) {
                System.err.println("Error in full game state listener: " + e.getMessage());
            }
        }).start();
    }

    // Send incremental updates to backups (primary server only)
    public void sendIncrementalUpdate(String message) {
        if (isPrimary && kafkaProducer != null) {
            ProducerRecord<String, String> record = new ProducerRecord<>("incremental-updates", message);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send incremental update to Kafka:" + exception.getMessage());
                }
                else {
                    System.out.println("Incremental update sent to Kafka: " + metadata);
                }
            });
        }
    }


    // Send full game state to backups (primary server only)
    public void sendFullGameState() {
        if (isPrimary && kafkaProducer != null) {
            String gameState = serializeGameState();
            ProducerRecord<String, String> record = new ProducerRecord<>("game-state", gameState);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("Failed to send full game state to Kafka: " + exception.getMessage());
                }
                else {
                    System.out.println("Full game state sent to Kafka: " + metadata);
                }
            });
        }
    }

    // Serialize the game state into a JSON string
    private String serializeGameState() {
        Gson gson = new Gson();
        Map<String, Object> gameState = new HashMap<>();
        gameState.put("activeGames", activeGames);

        //Create a map of users by their IDs
        Map<String,User> usersById = new HashMap<>();
        for (Map.Entry<WebSocket, User> entry : webServer.getConnectedUsers().entrySet()) {
            usersById.put(entry.getValue().getId(), entry.getValue());
        }
        gameState.put("connectedUsersById", usersById);
        gameState.put("temporarilyDisconnectedUsers", temporarilyDisconnectedUsers);
        return gson.toJson(gameState);
    }

    // Process incremental updates (secondary servers only)
    private void processIncrementalUpdate(String message) {
        System.out.println("Processing incremental update: " + message);
    
        // Simulate a WebSocket connection for the secondary server
        // Since secondary servers don't have real WebSocket connections for replicated messages,
        // we use a dummy WebSocket object.
        WebSocket dummyConn = new DummyWebSocket();
    
        // Handle the message in the same way as the primary server
        if (message.startsWith("/reconnect ")) {
            String userId = message.substring(11).trim();
            webServer.handleReconnect(dummyConn, userId);
        } else if (message.startsWith("/setname ")) {
            webServer.handleSetUsername(dummyConn, message.substring(9).trim());
        } else if (message.equals("/creategame")) {
            webServer.handleCreateGame(dummyConn);
        } else if (message.startsWith("/getgame ")) {
            String gameCode = message.substring(9).trim();
            webServer.handleGetGame(dummyConn, gameCode);
        } else if (message.startsWith("/join-game ")) {
            String gameCode = message.substring(11).trim();
            webServer.handleJoinGame(dummyConn, gameCode);
        } else if (message.startsWith("/word-selection ")) {
            String gameCode = message.substring(15).trim();
            webServer.handleGetWords(dummyConn, gameCode);
        } else if (message.startsWith("/startgame ")) {
            String[] parts = message.split(" ");
            String gameCode = parts[1];
            String userId = parts[2];
            webServer.handleStartGame(dummyConn, gameCode, userId);
        } else if (message.startsWith("/select-word ")) {
            String[] parts = message.split(" ");
            if (parts.length < 3) {
                System.err.println("ERROR: Invalid word selection format.");
                return;
            }
            String gameCode = parts[1];
            String selectedWord = parts[2];
            webServer.handleWordSelection(dummyConn, gameCode, selectedWord);
        } else if (message.startsWith("/drawer-joined ")) {
            String gameCode = message.substring(15).trim();
            webServer.handleDrawerJoined(dummyConn, gameCode);
        } else if (message.startsWith("/round-over ")) {
            String gameCode = message.substring(12).trim();
            Game game = activeGames.get(gameCode);
            if (game != null) {
                webServer.startNewRound(game);
            }
        } else if (message.startsWith("/endgame ")) {
            String gameCode = message.split(" ")[1];
            webServer.handleEndGame(dummyConn, gameCode);
        } else if (message.startsWith("/chat-history ")) {
            String[] parts = message.split(" ", 2);
            String gameCode = parts[1];
            webServer.handleChatRequest(dummyConn, gameCode);
        } else if (message.startsWith("/chat ")) {
            String[] parts = message.split(" ", 3);
            String gameCode = parts[1];
            String chatData = parts[2];
            webServer.handleChat(dummyConn, gameCode, chatData);
        } else if (message.startsWith("/canvas-update ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                System.err.println("ERROR: Invalid canvas update format.");
                return;
            }
            String gameCode = parts[1];
            String json = parts[2];
            webServer.handleCanvasUpdate(dummyConn, gameCode, json);
        } else if (message.startsWith("/clear-canvas")) {
            String gameCode = message.split(" ")[1];
            Game game = activeGames.get(gameCode);
            if (game != null) {
                game.clearCanvasHistory();
                webServer.broadcastToGame(game, "CANVAS_CLEAR");
            }
        } else if (message.startsWith("/getcanvas")) {
            String[] parts = message.split(" ");
            if (parts.length < 2) {
                System.err.println("ERROR: Invalid canvas history request format.");
                return;
            }
            String gameCode = parts[1];
            int lastIndex = Integer.parseInt(parts[2]);
            webServer.handleGetCanvasHistory(dummyConn, gameCode, lastIndex);
        } else {
            System.err.println("Unknown command in incremental update: " + message);
        }
    }

    // Update the game state (secondary servers only)
    private void updateGameState(String gameStateJson) {
        System.out.println("Raw gameStateJson: " + gameStateJson); // Debug log
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();

        // Deserialize the JSON string into a map
        Map<String, Object> gameState = gson.fromJson(gameStateJson, type);

        // Clear the existing game state
        activeGames.clear();
        connectedUsersById.clear();
        temporarilyDisconnectedUsers.clear();

        // Deserialize and update the active games
        Map<String, Game> deserializedActiveGames = gson.fromJson(gson.toJson(gameState.get("activeGames")), new TypeToken<Map<String, Game>>() {}.getType());
        activeGames.putAll(deserializedActiveGames);

        // Deserialize and update the connected users
        Map<String, User> deserializedConnectedUsersById = gson.fromJson(gson.toJson(gameState.get("connectedUsersById")), new TypeToken<Map<String, User>>() {}.getType());
        connectedUsersById.putAll(deserializedConnectedUsersById);

        // Deserialize and update the temporarily disconnected users
        Map<String, User> deserializedDisconnectedUsers = gson.fromJson(gson.toJson(gameState.get("temporarilyDisconnectedUsers")), new TypeToken<Map<String, User>>() {}.getType());
        temporarilyDisconnectedUsers.putAll(deserializedDisconnectedUsers);

        // // Restart timers for all active games
        for (Game game : activeGames.values()) {
            if (game.getTimeLeft() > 0) { // Only restart the timer if timeLeft is valid
                webServer.startRoundTimer(game); // Use Game's startRoundTimer method
            }
        }
        
        System.out.println("Game state deserialized and updated.");
    }
}
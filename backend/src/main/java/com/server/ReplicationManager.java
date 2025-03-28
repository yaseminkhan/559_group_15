package com.server;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ReplicationManager {

    private final WebServer webServer;
    private boolean isPrimary;
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
    private final Object gameStateLock = new Object();
    private Thread consumerThread;

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
            initializeKafkaProducer();
        } else {
            initializeKafkaConsumer();
        }
    }
    private void initializeKafkaProducer() {
        if (kafkaProducer != null) {
            System.out.println("Kafka producer is already initialized");
        }
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);
        System.out.println("Kafka producer initialized for primary server.");
    }

    private void initializeKafkaConsumer() {
        if (kafkaConsumer != null) {
            System.out.println("Kafka consumer is already initialized.");
            return;
        }
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "kafka:9092");
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "game-state-consumer-group-" + serverAddress); // Unique Group for each server
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumer = new KafkaConsumer<>(consumerProps);
        kafkaConsumer.subscribe(Arrays.asList("game-state", "incremental-updates"));

        consumerThread = new Thread(() -> {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(200));
                    System.out.println("Polling from Kafka");
                    for (ConsumerRecord<String, String> record : records) {
                        if (record.topic().equals("game-state")) {
                            updateGameState(record.value());
                        } else if (record.topic().equals("incremental-updates")) {
                            processIncrementalUpdate(record.value());
                        }
                    }
                }
            } catch (org.apache.kafka.common.errors.WakeupException e) {
                System.out.println("Kafka consumer wakeup triggered, shutting down.");
            } catch (Exception e) {
                System.err.println("Unexpected error in Kafka consumer: " + e.getMessage());
            } finally {
                kafkaConsumer.close();
                System.out.println("Kafka consumer closed.");
            }
        });
        consumerThread.start();
        System.out.println("Kafka consumer initialized for backup server.");
    }

    public void switchToPrimary() {
        if (kafkaConsumer != null) {
            stopKafkaConsumer();
        }
        if (kafkaProducer == null) {
            initializeKafkaProducer();
        }
        isPrimary = true;
        System.out.println("Switched to primary role. Kafka producer started.");
    }

    public void switchToBackup() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
            kafkaProducer = null;
        }
        if (kafkaConsumer == null) {
            initializeKafkaConsumer();
        }
        
        isPrimary = false;
        System.out.println("Switched to backup role. Kafka consumer started.");
    }

    public void stopKafkaConsumer() {
        if (kafkaConsumer != null) {
            System.out.println("Stopping Kafka consumer thread...");
            kafkaConsumer.wakeup(); // Trigger WakeupException in poll()
        }
    
        if (consumerThread != null && consumerThread.isAlive()) {
            try {
                consumerThread.join(); // Wait for consumer thread to terminate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        }
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
             // ===== DEBUG PRINTS =====
            System.out.println("\n===== DEBUG: PRIMARY SENDING GAME STATE =====");
            System.out.println("Connected Users:");
            for (Map.Entry<String, User> entry : connectedUsersById.entrySet()) {
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
        synchronized (gameStateLock) {
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
            } else if (message.startsWith("/game-ended")){
                String gameCode = message.substring(12).trim();
                Game game = activeGames.get(gameCode);
                if (game != null) {
                    activeGames.remove(gameCode);
                    resetKafkaConsumerOffsets(gameCode);
                    System.out.println("Game " + gameCode + " has been removed from backup server.");
                    game.cancelTimer();
                }
            } else {
                System.err.println("Unknown command in incremental update: " + message);
            }
        }
    }

    // Update the game state (secondary servers only)
    private void updateGameState(String gameStateJson) {
        synchronized (gameStateLock) {
            System.out.println("Raw gameStateJson: " + gameStateJson); // Debug log
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>() {}.getType();

            // Deserialize the JSON string into a map
            Map<String, Object> gameState = gson.fromJson(gameStateJson, type);

            // Clear the existing game state
            activeGames.clear();
            // connectedUsersById.clear();
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
            
            System.out.println("\n===== DEBUG: BACKUP RECEIVED GAME STATE =====");
            System.out.println("Connected Users:");
            for (Map.Entry<String, User> entry : connectedUsersById.entrySet()) {
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
            System.out.println("===== END BACKUP DEBUG =====\n");


            // // Restart timers for all active games
            for (Game game : activeGames.values()) {
                if (game.getTimeLeft() > 0) { // Only restart the timer if timeLeft is valid
                    webServer.startRoundTimer(game); // Use Game's startRoundTimer method
                }
            }
            
            System.out.println("Game state deserialized and updated.");
        }
    }

    //Reset consumer offsets by committing the offsets to the latest position
    private void resetKafkaConsumerOffsets(String gameCode) {
        if (kafkaConsumer != null) {
            kafkaConsumer.seekToEnd(kafkaConsumer.assignment());
            System.out.println("Kafka consumer offsets reset for game: " + gameCode);
        }
    }

    public ConcurrentHashMap<String, User> getConnectedUsersById() {
        return connectedUsersById;
    }

}
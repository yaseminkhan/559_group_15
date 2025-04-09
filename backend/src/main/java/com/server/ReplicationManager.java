package com.server;

import java.io.OutputStream;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
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
import com.google.gson.GsonBuilder;
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
    private final ConcurrentHashMap<String, List<Chat>> chatUpdate;
    private final ConcurrentHashMap<String, List<Game.CanvasUpdate>> gameCanvasUpdate;
    private OutputStream backupIncrOutput;
    private OutputStream backupFullGameOutput;
    private KafkaProducer<String, String> kafkaProducer;
    private KafkaConsumer<String, String> kafkaConsumer;
    private final Object gameStateLock = new Object();
    private Thread consumerThread;

    public ReplicationManager(WebServer webServer, boolean isPrimary, String serverAddress, int heartbeatPort, List<String> allServers,
                             ConcurrentHashMap<String, Game> activeGames,
                             ConcurrentHashMap<WebSocket, User> connectedUsers,
                             ConcurrentHashMap<String, User> temporarilyDisconnectedUsers,
                             ConcurrentHashMap<String, List<Chat>> chatUpdate,
                             ConcurrentHashMap<String, List<Game.CanvasUpdate>> gameCanvasUpdate) {
        this.webServer = webServer;
        this.isPrimary = isPrimary;
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
        this.activeGames = activeGames;
        this.connectedUsersById = new ConcurrentHashMap<>(); //Wrapper map since ReplicationManager does not have a websocket
        this.temporarilyDisconnectedUsers = temporarilyDisconnectedUsers;
        this.chatUpdate = chatUpdate;
        this.gameCanvasUpdate = gameCanvasUpdate;

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
        System.out.println("THIS IS ENV : " + System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        kafkaProducer = new KafkaProducer<>(producerProps);
        System.out.println("Kafka producer initialized for primary server.");
    }

    private Game deepCopyGame(Game game) {
        synchronized (game){
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(EventWrapper.class, new EventWrapperDeserializer())
                .create();
            return gson.fromJson(gson.toJson(game), Game.class);
        }
    }

    private void initializeKafkaConsumer() {
        if (kafkaConsumer != null) {
            System.out.println("Kafka consumer is already initialized.");
            return;
        }
        Properties consumerProps = new Properties();
        System.out.println("THIS IS ENV : " + System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "game-state-consumer-group-" + serverAddress); // Unique Group for each server
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        kafkaConsumer = new KafkaConsumer<>(consumerProps);
        kafkaConsumer.subscribe(Arrays.asList("game-state", "incremental-updates"));

        consumerThread = new Thread(() -> {
            try {
                while (true) {
                    if (isPrimary) {
                        System.out.println("Now primary — exiting Kafka consumer thread.");
                        break;
                    }
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(200));
                    //System.out.println("Polling from Kafka");
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

    public void sendIncrementalUpdate(String gameCode) {
        if (isPrimary && kafkaProducer != null) {
            Game game = activeGames.get(gameCode);
            if (game == null) {
                System.out.println("ERROR: Game not found for incremental update: " + gameCode);
                return;
            }

            // Create a map containing both canvas updates and chat updates
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("gameCode", gameCode);
            

            synchronized (gameCanvasUpdate) {
                gameCanvasUpdate.computeIfAbsent(gameCode, k -> new ArrayList<>());
                synchronized (gameCanvasUpdate.get(gameCode)) {
                    updateData.put("canvasUpdates", new ArrayList<>(gameCanvasUpdate.get(gameCode))); 
                }
            }

            synchronized (chatUpdate) {
                chatUpdate.computeIfAbsent(gameCode, k -> new ArrayList<>());
                synchronized (chatUpdate.get(gameCode)) {
                    updateData.put("chatUpdates", new ArrayList<>(chatUpdate.get(gameCode)));
                }
            }


            // Serialize to JSON
            Gson gson = new Gson();
            String message = gson.toJson(updateData);

            // Send to Kafka
            ProducerRecord<String, String> record = new ProducerRecord<>("incremental-updates", message);
            kafkaProducer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    System.err.println("No updates to send");
                } else {
                    System.out.println("Incremental update sent for game: " + gameCode);
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

        // Create a deep copy of the active games to avoid ConcurrentModificationException
        Map<String, Game> snapshot = new HashMap<>();
        synchronized (activeGames) {
            for (Map.Entry<String, Game> entry : activeGames.entrySet()) {
                snapshot.put(entry.getKey(), deepCopyGame(entry.getValue()));
            }
        }
        gameState.put("activeGames", snapshot);


        //Create a map of users by their IDs
        Map<String,User> usersById = new HashMap<>();
        for (Map.Entry<WebSocket, User> entry : webServer.getConnectedUsers().entrySet()) {
            usersById.put(entry.getValue().getId(), entry.getValue());
        }
        gameState.put("connectedUsersById", usersById);
        gameState.put("temporarilyDisconnectedUsers", temporarilyDisconnectedUsers);
        return gson.toJson(gameState);
    }

    private void processIncrementalUpdate(String json) {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> updateData = gson.fromJson(json, type);

            String gameCode = (String) updateData.get("gameCode");
            if (gameCode == null) {
                //System.out.println("ERROR: Invalid incremental update, missing gameCode");
                return;
            }

            Game game = activeGames.get(gameCode);
            if (game == null) {
                System.out.println("ERROR: Game not found for incremental update: " + gameCode);
                return;
            }

            // Deserialize canvas updates
            List<Game.CanvasUpdate> canvasUpdates = gson.fromJson(
                gson.toJson(updateData.get("canvasUpdates")),
                new TypeToken<List<Game.CanvasUpdate>>() {}.getType()
            );

            // Deserialize chat updates
            List<Chat> chatUpdates = gson.fromJson(
                gson.toJson(updateData.get("chatUpdates")),
                new TypeToken<List<Chat>>() {}.getType()
            );

            // Apply updates to game
            for (Game.CanvasUpdate update : canvasUpdates) {
                game.addCanvasUpdate(update);

                //System.out.println("Incremental canvas update applied");
            }

            // Apply chat updates to game, but check for duplicates based on userId and timestamp
            for (Chat update : chatUpdates) {
                boolean messageExists = false;

                // Check if a message from the same user with the same timestamp already exists
                for (Chat existingChat : game.getChatEvents()) {  
                    if (existingChat.getId().equals(update.getId()) &&
                        existingChat.getTimestamp() == (update.getTimestamp())) {
                        messageExists = true;
                        break;
                    }
                }

                if (!messageExists) {
                    game.addMessage(update);
                    //System.out.println("Incremental message update applied");
                } else {
                    System.out.println("Duplicate message ignored: User ID = " + update.getId() + ", Timestamp = " + update.getTimestamp());
                }
            }
        } catch (Exception e) {
            System.err.println("No incremental updates to apply");
        }
    }


    // Update the game state (secondary servers only)
    private void updateGameState(String gameStateJson) {
        synchronized (gameStateLock) {
            System.out.println("Raw gameStateJson: " + gameStateJson); // Debug log
            // Gson gson = new Gson();
            Gson gson = new GsonBuilder()
                .registerTypeAdapter(EventWrapper.class, new EventWrapperDeserializer())
                .create();
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
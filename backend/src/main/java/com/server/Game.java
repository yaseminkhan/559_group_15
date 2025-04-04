package com.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import com.google.gson.Gson;

/*
 * Game instance
 */
public class Game {
    private static final int MAX_PLAYERS = 8; // max players set to 8 for now 

    private String gameCode;
    private List<User> players;
    private Set<String> confirmedEndGame;
    private User drawer;
    private int round;
    private String wordToDraw;
    private boolean gameStarted;
    private boolean gameEnded;
    private int drawerIndex;
    private int timeLeft;
    private transient Timer roundTimer; // Transient so Gson ignores it wben seri
    private boolean roundStarted;

    private List<EventWrapper> eventHistory;
    private final LogicalClock logicalClock;

    /*
     * constructor creates new instance of a game
     */
    public Game(String gameCode) {
        this.gameCode = gameCode;
        this.players = new ArrayList<>();
        this.confirmedEndGame = new HashSet<>();
        this.gameStarted = false;
        this.gameEnded = false;
        this.drawer = null;
        this.drawerIndex = -1;
        this.wordToDraw = Words.getRandomWord();
        this.timeLeft = 60;
        this.round = 1;
        this.roundStarted = false;
        this.eventHistory = Collections.synchronizedList(new ArrayList<>());
        this.logicalClock = new LogicalClock();
    }

    public synchronized void addEvent(Event event) {
        String type;
        if (event instanceof Chat) {
            type = "CHAT";
        } else if (event instanceof CanvasUpdate) {
            type = "CANVAS";
        } else if (event instanceof CanvasClear) {
            type = "CLEAR";
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass());
        }
    
        EventWrapper wrapper = new EventWrapper(type, event);
        eventHistory.add(wrapper);
    }

    public LogicalClock getLogicalClock() {
        return logicalClock;
    }

    public List<Chat> getChatEvents() {
        return eventHistory.stream()
                .filter(e -> "CHAT".equals(e.type))
                .map(e -> (Chat) e.data)
                .sorted()
                .toList();
    }
    
    public List<CanvasUpdate> getCanvasEvents() {
        int lastClearIndex = -1;
    
        synchronized (eventHistory) {
            for (int i = eventHistory.size() - 1; i >= 0; i--) {
                EventWrapper e = eventHistory.get(i);
                if ("CLEAR".equals(e.type)) {
                    lastClearIndex = i;
                    break;
                }
            }
    
            return eventHistory.subList(lastClearIndex + 1, eventHistory.size()).stream()
                    .filter(e -> "CANVAS".equals(e.type))
                    .map(e -> (CanvasUpdate) e.data)
                    .toList();
        }
    }

    public void clearEvents() {
        synchronized (eventHistory) {
            eventHistory.clear();
        }
    }

    public void clearGame() {
        this.gameCode = null;
        players.clear();
        confirmedEndGame.clear();
        this.drawer = null;
        this.wordToDraw = null;
        clearEvents(); // clear all events for next game
        for (var player : players) {
            player.setGameCode(null);
            player.setWasDrawer(false);
            player.setAlreadyGuessed(false);
            player.setScore(0);
            player.setUsername(null);
            player.setIsHost(false);
        }
    }

    private int getPlayersAlreadyGuessed() {
        int total = 0;
        for (var player : players)
            if (player.getAlreadyGuessed())
                ++total;
        return total;
    }

    public void cancelTimer() {
        if (roundTimer != null) {
            roundTimer.cancel();
            roundTimer.purge();
            roundTimer = null;
        }
    }

    public void resetForRound() {
        clearEvents(); // clear events for next round 
        for (var player : players)
            player.setAlreadyGuessed(false);
        cancelTimer();
    }

    public User getUserById(String id) {
        User user = null;
        for (var player : players) {
            if (player.getId().equals(id)) {
                user = player;
                break;
            }
        }
        return user;
    }

    private int calcScore() {
        return (1 << (players.size() - getPlayersAlreadyGuessed())) // Position bonus (exponential decay).
                + timeLeft; // Timing bonus.
    }

    public Chat addMessage(Chat message) {
        var user = getUserById(message.id);

        message.text = message.text.trim(); // Ignore leading and trailing whitespace.
        message.sender = user.getUsername();

        if (!user.getAlreadyGuessed()) {
            if (message.text.equalsIgnoreCase(wordToDraw)) {
                user.setScore(user.getScore() + calcScore());
                user.setAlreadyGuessed(true);
                message.text = user.getUsername() + " guessed correctly!";
                message.correct = true;
            }
            addEvent(message); 
            // System.out.println("Message sender: " + message.sender);
            // System.out.println("Message: " + message);
        }    
        return message;
    }

    public String getWordToDraw() {
        return wordToDraw;
    }

    /*
     * Adds new players to a game as long as there is space and game has not started 
     * Returns boolean indicating result 
     */
    public boolean addPlayer(User player) {
        if (players.size() >= MAX_PLAYERS) {
            System.out.println("Game is full. Cannot add " + player.getUsername());
            return false;
        }
        if (!gameStarted) {
            if (players.isEmpty()) {
                player.setHost(); // First player is the host
            }
            players.add(player);
            return true;
        }
        return false;
    }

    /*
     * Checks if the given player is already in the game.
     */
    public boolean hasPlayer(User player) {
        return players.contains(player);
    }

    /*
     * Checks if all players have guessed correctly.
     */
    public boolean allPlayersGuessed() {
        for (var player : players) {
            if (player.isDrawer())
                continue;
            if (!player.getAlreadyGuessed())
                return false;
        }
        return true;
    }

    /*
     * Removes player from game and handles situation where the lost player is the drawer 
     */
    public void removePlayer(User player) {
        players.remove(player);
    }

    public void setGameStarted(boolean started) {
        this.gameStarted = started;
        System.out.println("GameStarted set to: " + started);
    }

    /*
     * starts a new game 
     */
    public boolean startGame(User user) {
        if (!players.isEmpty() && user.isHost()) {
            // resetRoundPoints();
            gameStarted = true;
            round = 1;
            assignNextDrawer();
            // Reset scores to 0 for all players
            for (User player : players) {
                player.setScore(0);
                // Reset boolean indicating if player has been the drawer already 
                player.setWasDrawer(false);
            }
            return true;
        }
        return false; // Game cannot start if not host
    }

    /* 
     * Assigns new drawer
     */
    public void assignNextDrawer() {
        if (players.isEmpty()) {
            System.out.println("No players left in the game.");
            return;
        }

        for (User player : players) {
            player.removeAsDrawer(); // Set all players to isDrawer = false
        }

        if (drawer != null) {
            System.out.println("Previous drawer: " + drawer.getUsername());
            drawer.removeAsDrawer();
            drawer = null; // Ensure the old drawer is cleared
        }

        if (hasAvailableDrawer()) {
            System.out.println("\n--- Assigning New Drawer ---");
            for (User player : players) {
                System.out.println(player.getUsername() + " - wasDrawer: " + player.wasDrawer() + ", isDrawer: "
                        + player.isDrawer());
            }

            // Select the next available drawer
            for (int i = 0; i < players.size(); i++) {
                drawerIndex = (drawerIndex + 1) % players.size();
                User potentialDrawer = players.get(drawerIndex);

                if (!potentialDrawer.wasDrawer()) { // Find the first player who hasn’t drawn
                    drawer = potentialDrawer;
                    drawer.setDrawer();
                    drawer.setWasDrawer(true);

                    System.out.println("\n==========================================\n");
                    System.out.println("New drawer assigned: " + drawer.getUsername());
                    System.out.println("==========================================\n");

                    wordToDraw = selectRandomWord();
                    return;
                }
            }
        }
    }

    /*
     * Determine if there is any available players who have not drawn yet 
     */
    public boolean hasAvailableDrawer() {
        System.out.println("\n--- Checking Available Drawers ---");

        if (players.isEmpty()) {
            System.out.println("No players left in the game.");
            return false;
        }

        for (User player : players) {
            //System.out.println(player.getUsername() + " - wasDrawer: " + player.wasDrawer() + ", isDrawer: " + player.isDrawer());

            if (!player.wasDrawer()) {
                return true;
            }
        }

        return false;
    }

    public boolean isRoundStarted() {
        return roundStarted;
    }

    public void setRoundStarted(boolean roundStarted) {
        this.roundStarted = roundStarted;
    }

    /*
     * Gets a random word to use 
     */
    private String selectRandomWord() {
        return Words.getRandomWord();
    }

    /*
     * Gets 4 random words to use - will implement later 
     */
    public List<String> getWordChoices() {
        return Words.getRandomWordChoices();
    }

    /*
     * moves to next round 
     */
    public void nextTurn() {
        round++; // Increase round count
        System.out.println("Starting round " + round);
        assignNextDrawer(); // Assign new drawer

        if (drawer != null) {
            System.out.println("New drawer for this round: " + drawer.getUsername());
        } else {
            System.out.println("ERROR: No drawer assigned, this should not happen.");
        }

        this.timeLeft = 60;
    }

    /*
     * Get the list of players in the game.
     */
    public List<User> getPlayers() {
        return players;
    }

    /*
     * Add the user to set when they confirm to end game.
     */
    public void confirmEndGame(String userId) {
        confirmedEndGame.add(userId);
    }

    /*
     * Returns number of users in game that have confirmed end game.
     */
    public int sizeOfPlayersConfirmedEnd() {
        return confirmedEndGame.size();
    }

    /*
     * Sets the time left for the round
     */
    public void setTimeLeft(int time) {
        this.timeLeft = time;
    }

    public void setTimer(Timer t) {
        this.roundTimer = t;
    }

    public Timer getTimer() {
        return roundTimer;

    }

    public boolean hasGameStarted() {
        return this.gameStarted;
    }

    public boolean isGameEnded() {
        return this.gameEnded;
    }

    /*
     * Converts player list to JSON format for frontend.
     */
    public String getPlayersJson() {
        Gson gson = new Gson();
        List<Map<String, Object>> playerList = new ArrayList<>();

        for (User player : players) {
            Map<String, Object> playerData = new HashMap<>();
            playerData.put("id", player.getId());
            playerData.put("username", player.getUsername());
            playerData.put("icon", player.getIcon());
            playerData.put("score", player.getScore());
            playerData.put("isDrawer", player.isDrawer());
            playerData.put("isHost", player.isHost());
            playerList.add(playerData);
        }
        return gson.toJson(playerList);
    }

    public boolean isRoundInProgress() {
        return roundStarted
                && !gameEnded
                && gameStarted
                && wordToDraw != null
                && !wordToDraw.isEmpty()
                && drawer != null
                && timeLeft > 0;
    }

    /*
    * Resets all player scores to 0.
    */
    public void resetScores() {
        for (User player : players) {
            player.setScore(0);
        }
        System.out.println("All player scores have been reset to 0.");
    }

    /*
     * ends the game
     */
    public void endGame() {
        gameEnded = true;
        System.out.println("Game Over! Final Scores:");
        for (User player : players) {
            System.out.println(player.getUsername() + ": " + player.getScore() + " points");
            player.setWasDrawer(false);
        }
        // players.clear();
    }

    // functions to get game information 
    public boolean isFull() {
        return players.size() >= MAX_PLAYERS;
    }

    public boolean hasEnded() {
        return gameEnded;
    }

    public int getCurrentRound() {
        return round;
    }

    public String getGameCode() {
        return this.gameCode;
    }

    public User getDrawer() {
        return this.drawer;
    }

    public int getTimeLeft() {
        return this.timeLeft;
    }

    /*
     * set current word
     */
    public void setCurrentWord(String word) {
        this.wordToDraw = word;
    }

    public List<Chat> getChatMessages() {
        return chatMessages;
    }

    /*
    * Canvas History Functions
    */
    public void addCanvasUpdate(CanvasUpdate update) {
        //canvasHistory.add(update);
        addEvent(update);
    }

    // Class for CanvasUpdate
    public static class CanvasUpdate extends Event {
        private double x;
        private double y;
        private String color;
        private double width;
        private boolean newStroke;

        public CanvasUpdate() {} // Required for deserialization

        public CanvasUpdate(double x, double y, String color, double width, boolean newStroke) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.width = width;
            this.newStroke = newStroke;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public String getColor() {
            return color;
        }

        public double getWidth() {
            return width;
        }

        public boolean getNewStroke() {
            return newStroke;
        }
    }
}
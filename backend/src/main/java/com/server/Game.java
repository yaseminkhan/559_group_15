package com.server;

import java.util.*;
import com.google.gson.Gson;

/*
 * Game instance
 */
public class Game {
    private static final int MAX_PLAYERS = 8; // max players set to 8 for now 

    private String gameCode;
    private List<User> players;
    private Set<String> confirmedEndGame;
    private Set<String> drawnPlayers;
    private User drawer;
    private int round;
    private String wordToDraw;
    private boolean gameStarted;
    private boolean gameEnded;
    private List<Chat> chatMessages;
    private int drawerIndex;
    private int timeLeft;
    private int maxRounds;
    private Timer roundTimer;
    
        private List<CanvasUpdate> canvasHistory;
    
        /*
         * constructor creates new instance of a game
         */
        public Game(String gameCode) {
            this.gameCode = gameCode;
            this.players = new ArrayList<>();
            this.confirmedEndGame = new HashSet<>();
            this.drawnPlayers = new HashSet<>();
            this.round = 1;
            this.gameStarted = false;
            this.gameEnded = false;
            this.chatMessages = new ArrayList<>();
            this.drawer = null;
            this.drawerIndex = -1;
            this.maxRounds = 1; // Default value, will be updated when the game starts
            this.wordToDraw = Words.getRandomWord();
            this.timeLeft = 60;
            this.canvasHistory = new ArrayList<>();
    
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
            chatMessages.clear();
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
    
        /*
         * Updates the number of rounds to match the number of players.
         */
        public void updateMaxRounds() {
            this.maxRounds = players.size();
        }
    
        public Chat addMessage(Chat message) {
            var user = getUserById(message.id);
    
            if (!user.getAlreadyGuessed()) {
                if (message.text.equalsIgnoreCase(wordToDraw)) {
                    user.setScore(user.getScore() + (1 << (players.size() - getPlayersAlreadyGuessed())));
                    user.setAlreadyGuessed(true);
                    message.text = user.getUsername() + " guessed correctly!"; // Text is just modified to say the user guessed correctly.
                    message.correct = true;
                }
                chatMessages.add(message); // Messages are only added when someone hasn't yet guessed correctly.
            }
    
            System.out.println("----------------SCORE BOARD-------------------");
            for (var player : players) {
                System.out.printf("player: %s, score: %d\n", player.getUsername(), player.getScore());
            }
            System.out.println("----------------------------------------------");
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
            if (player.isDrawer()) {
                nextTurn(); // Auto-assign new drawer
            }
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
    
        public Set<String> getDrawnPlayers() {
            return this.drawnPlayers;
        }
    
        /* 
         * Assigns new drawer - will need to change to a different algorithm later 
         */
        public void assignNextDrawer() {
            if (players.isEmpty()) {
                System.out.println("No players left in the game.");
                return;
            }
    
            if (drawer != null) {
                System.out.println("Previous drawer: " + drawer.getUsername());
                drawer.setDrawer();
                drawer.removeAsDrawer();
            }
    
            if(hasAvailableDrawer()){
                System.out.println("\n--- Assigning New Drawer ---");
                for (User player : players) {
                     System.out.println(player.getUsername() + " - wasDrawer: " + player.wasDrawer() + ", isDrawer: " + player.isDrawer());
                }
                // Select the next available drawer
                for (int i = 0; i < players.size(); i++) {
                    drawerIndex = (drawerIndex + 1) % players.size();
                    User potentialDrawer = players.get(drawerIndex);

                    if (!potentialDrawer.wasDrawer()) { // Find the first player who hasn’t drawn
                        drawer = potentialDrawer;
                        drawer.setDrawer();
                        System.out.println("------------------------------------------------\n" );
                        System.out.println("\nsetting current drawer as has been drawer" + drawer.getUsername());
                        System.out.println("------------------------------------------------\n" );
                        drawer.setWasDrawer(true);
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
        
            boolean foundDrawer = false;
            
            for (User player : players) {
                System.out.println(player.getUsername() + " - wasDrawer: " + player.wasDrawer() + ", isDrawer: " + player.isDrawer());
                
                if (!player.wasDrawer()) {
                    foundDrawer = true;
                }
            }
        
            if (!foundDrawer) {
                System.out.println("All players have already drawn. Ending game.");
            }
            
            return foundDrawer;
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
            if (!hasAvailableDrawer()) {
                System.out.println("No available drawers. Ending game.");
                endGame();
            endGame();
                return;
            }
    
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
            }
            players.clear();
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
    
        public int getMaxRounds() {
            return this.maxRounds;
        }

    /*
     * set current word
     */
    public void setCurrentWord(String word) {
        this.wordToDraw = word;
    }

    public Object getChatMessages() {
        return chatMessages;
    }

    /*
    * Canvas History Functions
    */
    public void addCanvasUpdate(CanvasUpdate update) {
        canvasHistory.add(update);
    }

    public List<CanvasUpdate> getCanvasHistory() {
        return new ArrayList<>(canvasHistory);
    }

    public void clearCanvasHistory() {
        canvasHistory.clear();
    }

    // Class for CanvasUpdate
    public static class CanvasUpdate {
        private double x;
        private double y;
        private String color;
        private double width;
        private boolean newStroke;

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

    public void setTimer(Timer t){
        this.roundTimer = t;
    }
}

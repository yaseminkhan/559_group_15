package com.server;

import java.util.*;
import com.google.gson.Gson;

/*
 * Game instance
 */
public class Game {
    private static final int MAX_PLAYERS = 8; // max players set to 8 for now 
    private static final int MAX_ROUNDS = 3; // max rounds set to 8 for now 

    private String gameCode;
    private List<User> players;
    private Set<String> confirmedEndGame;
    private Set<String> drawnPlayers;
    private User drawer;
    private int round;
    private String wordToDraw;
    private boolean gameStarted;
    private boolean gameEnded;
    private List<Stroke> strokes;
    // private List<String> chatMessages;
    private List<Chat> chatMessages;
    private int drawerIndex;
    private int timeLeft;

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
        this.strokes = new ArrayList<>();
        this.chatMessages = new ArrayList<>();
        this.drawer = null;
        this.drawerIndex = -1;
        this.wordToDraw = Words.getRandomWord();
        this.timeLeft = 60;
    }

    private int getPlayersAlreadyGuessed() {
        int total = 0;
        for (var player : players)
            if (player.getAlreadyGuessed())
                ++total;
        return total;
    }

    public User getUserByName(String username) {
        User ret = null;
        for (var player : players) {
            if (player.getUsername().equals(username)) {
                ret = player;
                break;
            }
        }
        return ret;
    }

    public Chat addMessage(Chat message) {
        var user = getUserByName(message.sender);

        if (!user.getAlreadyGuessed() && message.text.equalsIgnoreCase(wordToDraw)) {
            user.setScore(user.getScore() + (1 << (players.size() - getPlayersAlreadyGuessed())));
            user.setAlreadyGuessed(true);
            message.correct = true;
        }

        chatMessages.add(message);
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

    public List<Stroke> getStrokes() {
        return strokes;
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

    // private void resetRoundPoints() {
    //     roundPoints = IntStream
    //             .range(0, players.size())
    //             .reduce((x, y) -> x + (1 << y))
    //             .getAsInt();
    // }

    /*
     * starts a new game 
     */
    public boolean startGame(User user) {
        if (!players.isEmpty() && user.isHost()) {
            // resetRoundPoints();
            gameStarted = true;
            round = 1;
            assignNextDrawer();
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
            return;
        }

        if (drawer != null) {
            drawer.removeAsDrawer();
        }

        // Case when only 2 players - allow alternating turns
        if (players.size() == 2) {
            drawerIndex = (drawerIndex + 1) % players.size();
        } else {
            // Find the next player who hasn't drawn yet
            int startIndex = drawerIndex;
            do {
                drawerIndex = (drawerIndex + 1) % players.size();
            } while (drawnPlayers.contains(players.get(drawerIndex).getId()) && drawerIndex != startIndex);

            // Mark the new drawer as having drawn
            drawnPlayers.add(players.get(drawerIndex).getId());
        }

        drawer = players.get(drawerIndex);
        drawer.setDrawer();
        wordToDraw = selectRandomWord();
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
        if (round > MAX_ROUNDS) {
            endGame();
            return;
        }

        round++; // Increase round count
        System.out.println("Starting round " + round);
        assignNextDrawer(); // Assign new drawer
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

    public static int getMaxRounds() {
        return MAX_ROUNDS;
    }

    /*
     * set current word
     */
    public void setCurrentWord(String word) {
        this.wordToDraw = word;
    }
}

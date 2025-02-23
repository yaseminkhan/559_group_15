package com.server;

import java.util.*;
import com.google.gson.Gson;
import com.server.Words;

/*
 * Game instance
 */
public class Game {
    private static final int MAX_PLAYERS = 8; // max players set to 8 for now 
    private static final int MAX_ROUNDS = 3; // max rounds set to 8 for now 

    private String gameCode;
    private List<User> players;
    private User drawer;
    private int round;
    private String wordToDraw;
    private boolean gameStarted;
    private boolean gameEnded;
    // private List<Stroke> strokes;
    private List<String> chatMessages;
    private int drawerIndex;

    /*
     * constructor creates new instance of a game
     */
    public Game(String gameCode) {
        this.gameCode = gameCode; 
        this.players = new ArrayList<>();
        this.round = 0;
        this.gameStarted = false;
        this.gameEnded = false;
        // this.strokes = new ArrayList<>();
        this.chatMessages = new ArrayList<>();
        this.drawer = null;
        this.drawerIndex = -1;
        this.wordToDraw = Words.getRandomWord();
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
    public void startGame() {
        if (!players.isEmpty()) {
            gameStarted = true;
            round = 1;
            assignNextDrawer();
        }
    }

    /* 
     * Assigns new drawer - will need to change to a different algorithm later 
     */
    private void assignNextDrawer() {
        if (!players.isEmpty()) {
            if (drawer != null) {
                drawer.removeAsDrawer();
            }

            // Move to the next player in order
            drawerIndex = (drawerIndex + 1) % players.size();
            drawer = players.get(drawerIndex);
            drawer.setDrawer();
            wordToDraw = selectRandomWord();
        }
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
        if (round >= MAX_ROUNDS) {
            endGame();
            return;
        }
        assignNextDrawer();
        round++;
    }

    /*
     * Get the list of players in the game.
     */
    public List<User> getPlayers() {
        return players;
    }
    
    /*
     * Converts player list to JSON format for frontend.
     */
    public String getPlayersJson() {
        Gson gson = new Gson();
        return gson.toJson(players);
    }

    /*
     * ends the game - we need to create a page for this 
     */
    public void endGame() {
        gameEnded = true;
        System.out.println("Game Over! Final Scores:");
        for (User player : players) {
            System.out.println(player.getUsername() + ": " + player.getScore() + " points");
        }
    }

    // functions to get game information 
    public boolean isFull() {return players.size() >= MAX_PLAYERS;}
    public boolean hasEnded() {return gameEnded;}
    public int getCurrentRound() {return round;}
}

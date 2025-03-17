package com.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/*
 * Represents players 
 */
public class User {
    // list of emojis to randomly assign to users 
    private static final List<String> EMOJIS = Arrays.asList(
            "🐌", "🌵", "😃", "☀️", "🦃", "💬", "📚", "🥶",
            "🚀", "🎸", "🎨", "⚡", "🎭", "🍕", "🐙");


    private static final Set<String> usedEmojis = new HashSet<>(); // Set to keep track of used emojis
    private String id;
    private String username;
    private String icon;
    private int score;
    private boolean isDrawer;
    private boolean isHost;
    private boolean alreadyGuessed; // Guessed correctly.
    private String gameCode;

    /*
     * Contructor to create a new user 
     */
    public User(String username) {
        this.id = UUID.randomUUID().toString();
        this.username = username;
        this.icon = setIcon();
        this.score = 0;
        this.isDrawer = false; // default
        this.isHost = false; // default
        this.alreadyGuessed = false;
    }

     /*
     * Assigns a unique emoji to the user
     */
    private String setIcon() {
        List<String> availableEmojis = new ArrayList<>();    
        
        // Check which emojis have not been used yet
        
        for (String emoji : EMOJIS) {
            if (!usedEmojis.contains(emoji)) {
                availableEmojis.add(emoji);
            }
        }

        if (availableEmojis.isEmpty()) {
            throw new IllegalStateException("No more unique emojis available!");
        }

        // Randomly pick an available emoji
        Random rand = new Random();
        String selectedEmoji = EMOJIS.get(rand.nextInt(EMOJIS.size()));
        usedEmojis.add(selectedEmoji); // Mark the emoji as used
        
        return selectedEmoji;
    }


    /*
     * Check if the user has already guessed correctly.
     */
    public boolean getAlreadyGuessed() {
        return alreadyGuessed;
    }

    /*
     * Set the alreadyGuessed value.
     */
    public void setAlreadyGuessed(boolean val) {
        alreadyGuessed = val;
    }

    /*
     * Set user score.
     */
    public void setScore(int value) {
        if (value < 0)
            throw new IllegalArgumentException("Can't assign negative score to player");
        score = value;
    }

    /*
     * sets username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    // functions to return information about users 
    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getIcon() {
        return icon;
    }

    public int getScore() {
        return score;
    }

    public boolean isDrawer() {
        return isDrawer;
    }

    public boolean isHost() {
        return isHost;
    }

    public String getGameCode() {
        return gameCode;
    }

    // functions to modify users points and assign a drawer 
    public void setDrawer() {
        this.isDrawer = true;
    }

    public void removeAsDrawer() {
        this.isDrawer = false;
    }

    public void addPoints(int points) {
        this.score += points;
    }

    public void setHost() {
        this.isHost = true;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }

    // helps printing users for debugging 
    @Override
    public String toString() {
        return username + " (" + icon + ")" + (isDrawer ? " [DRAWER]" : "");
    }
}

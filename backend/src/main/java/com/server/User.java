package com.server;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/*
 * Represents players 
 */
public class User {
    // list of emojis to randomly assign to users 
    private static final List<String> EMOJIS = Arrays.asList(
        "🐌", "🌵", "😃", "☀️", "🦃", "💬", "📚", "🥶",
        "🚀", "🎸", "🎨", "⚡", "🎭", "🍕", "🐙"
    );

    private String id;
    private String username;
    private String icon;
    private int score;
    private boolean isDrawer;
    private boolean isHost;
    private String gameCode;

    /*
     * Contructor to create a new user 
     */
    public User(String username){
        this.id = UUID.randomUUID().toString(); 
        this.username = username;
        this.icon = getRandomEmoji();
        this.score = 0;
        this.isDrawer = false; // default
        this.isHost = false; // default
    }

    /*
     * Returns a random emoji from the list
     */
    private String getRandomEmoji() {
        Random rand = new Random();
        return EMOJIS.get(rand.nextInt(EMOJIS.size()));
    }

    /*
     * sets username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    // functions to return information about users 
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getIcon() { return icon; }
    public int getScore() { return score; }
    public boolean isDrawer() { return isDrawer; }
    public boolean isHost() { return isHost; }
    public String getGameCode() {return gameCode; }

    // functions to modify users points and assign a drawer 
    public void setDrawer() { this.isDrawer = true; }
    public void removeAsDrawer() { this.isDrawer = false; }
    public void addPoints(int points) { this.score += points; }
    public void setHost() { this.isHost = true; } 
    public void setGameCode(String gameCode) { this.gameCode = gameCode; }

    // helps printing users for debugging 
    @Override
    public String toString() {
        return username + " (" + icon + ")" + (isDrawer ? " [DRAWER]" : "");
    }
}

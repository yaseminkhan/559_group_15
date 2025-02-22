package com.server;

import java.util.UUID;

/*
 * Represents players 
 */
public class User {
    private String id;
    private String username;
    private String icon;
    private int score;
    private boolean isDrawer;

    /*
     * Contructor to create a new user 
     */
    public User(String username, String icon){
        this.id = UUID.randomUUID().toString(); 
        this.username = username;
        this.icon = icon;
        this.score = 0;
        this.isDrawer = false; // default
    }

    // functions to return information about users 
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getIcon() { return icon; }
    public int getScore() { return score; }
    public boolean isDrawer() { return isDrawer; }

    // functions to modify users points and assign a drawer 
    public void setDrawer() { this.isDrawer = true; }
    public void removeAsDrawer() { this.isDrawer = false; }
    public void addPoints(int points) { this.score += points; }

    // helps printing users for debugging 
    @Override
    public String toString() {
        return username + " (" + icon + ")" + (isDrawer ? " [DRAWER]" : "");
    }
}

package com.server;

public class CanvasClear extends Event {
    private String gameCode;

    public CanvasClear() {} // Required for Gson

    public String getGameCode() {
        return gameCode;
    }

    public void setGameCode(String gameCode) {
        this.gameCode = gameCode;
    }
}
package com.server;

// THIS IS NOT BEING USED RIGHT NOW
// CAN HELP WITH DEALING WITH ADDRESS MISSMATCH 
public class ServerInfo {
    private final String ip;
    private final int port;
    private final int serverId;
    private long lastHeartbeatTime;

    public ServerInfo(String ip, int port, int serverId) {
        this.ip = ip;
        this.port = port;
        this.serverId = serverId;
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getServerId() {
        return serverId;
    }

    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }

    public void updateHeartbeatTime() {
        this.lastHeartbeatTime = System.currentTimeMillis();
    }

    public boolean isAlive() {
        return System.currentTimeMillis() - lastHeartbeatTime < HeartBeatManager.HEARTBEAT_TIMEOUT;
    }
}
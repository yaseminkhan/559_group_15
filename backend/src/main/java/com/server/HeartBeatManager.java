package com.server;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.List;

public class HeartBeatManager {
    private final String serverAddress;
    private final int heartbeatPort; //Port for heartbeat communication
    private final List<String> allServers; //List of all server addresses
    private long lastHeartbeatTime = System.currentTimeMillis(); //Timestamp of last received heartbeat

    public HeartBeatManager(String serverAddress, int heartbeatPort, List<String> allServers) {
        this.serverAddress = serverAddress;
        this.heartbeatPort = heartbeatPort;
        this.allServers = allServers;
    }

    //Send heartbeats to all peer servers
    public void sendHeartbeatToAllServers() {
        for (String server : allServers) {
            if (!server.equals(serverAddress)) { //Exclude self
                sendHeartbeat(server, heartbeatPort);
            }
        }

    }
    
    //Send heartbeats to a specific server
    public void sendHeartbeat(String serverIp, int port) {
        try (Socket socket = new Socket(serverIp, port)) {
            OutputStream output = socket.getOutputStream(); //Create output stream to send data
            output.write("HEARTBEAT".getBytes()); //Send the heartbeat message
            System.out.println("Heartbeat sent to server: " + serverIp);
        } catch (IOException ioe) {
            System.err.println("Failed to send heartbeat to " + serverIp + ": " + ioe.getMessage());
        }
    }

    //Start listening for heartbeats from other servers
    public void startHeartbeatListener(int port) {
        new Thread(() -> {
            try(ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Listening for heartbeats on port " + port);
                while (true) {
                    Socket socket = serverSocket.accept(); //Accept incoming connections
                    InputStream input = socket.getInputStream(); //Create input stream to read heartbeat of other servers
                    byte[] buffer = new byte[1024];

                    //Read incoming message
                    int bytesRead = input.read(buffer);
                    if (bytesRead > 0) {
                        String message = new String(buffer, 0, bytesRead);
                        if ("HEARTBEAT".equals(message)) {
                            //Update the timestamp of the last received heartbeat
                            lastHeartbeatTime = System.currentTimeMillis();
                            System.out.println("Heartbeat received from: " + socket.getInetAddress());
                        }
                    }
                    socket.close(); //Close socket after processing
                }
            } catch (IOException ioe) {
                System.err.println("Error in heartbeat listener: " + ioe.getMessage());
            }
        }).start();
    }

    //Start sending heartbeats to other servers periodically
    public void startHeartbeatSender() {
        new Thread(() -> {
            while (true) {
                sendHeartbeatToAllServers();
                try {
                    Thread.sleep(5000); //Send heartbeat every 5 seconds
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }).start();
    }

    //Get timestamp of the lat received heartbeat
    public long getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }
}

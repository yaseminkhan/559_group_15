package com.server;

/**
 * ServerProto.java
 * 
 * Testing file - implementations should be handled in a different file 
 *
 * @author Jin Song
 * @version 0.0.1
 * 
 */

import java.io.*;
import java.net.*;

public class ServerProto {

    private int port;
    
    public ServerProto(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        int port = 12345;
        ServerProto server = new ServerProto(port);
        server.start();
    
    }

    // For demonstration
    private static class ClientHandler implements Runnable {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("Connected! You are connected from: " + socket.getInetAddress());
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
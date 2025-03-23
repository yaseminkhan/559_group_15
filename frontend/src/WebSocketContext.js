import React, { createContext, useContext, useState, useEffect, useRef } from "react";

const WebSocketContext = createContext(null);
let gameSocket = null;

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState("ws://localhost:8887");

    const coordinatorRef = useRef(null);

    // Main game socket to backend server
    useEffect(() => {
        const connectGameSocket = () => {
            if (!gameSocket || gameSocket.readyState === WebSocket.CLOSED) {
                console.log(`Server address: ${serverAddress}`);
                gameSocket = new WebSocket(serverAddress);

                gameSocket.onopen = () => {
                    console.log(`Connected to backend: ${serverAddress}`);
                    setIsConnected(true);

                    const storedUserId = localStorage.getItem("userId");
                    if (storedUserId) {
                        console.log(`Reconnecting as user: ${storedUserId}`);
                        gameSocket.send(`/reconnect ${storedUserId}`);
                    }
                };

                gameSocket.onmessage = (event) => {
                    const message = event.data;
                    if (message.startsWith("USER_ID:")) {
                        const userId = message.split(":")[1];
                        console.log(`Received user ID: ${userId}`);
                        localStorage.setItem("userId", userId);
                    }
                };

                gameSocket.onerror = (error) => {
                    console.error("Game WebSocket error:", error);
                };

                gameSocket.onclose = () => {
                    console.warn("Game WebSocket closed. Reconnecting...");
                    gameSocket.close();
                    setIsConnected(false);
                    setTimeout(connectGameSocket, 3000);
                };

                setSocket(gameSocket);
            }
        };

        connectGameSocket();
    }, [serverAddress]);

    // Coordinator connection with auto-reconnect
    useEffect(() => {
        const connectCoordinator = () => {
            const coordinator = new WebSocket("ws://localhost:9999");

            coordinator.onopen = () => {
                console.log("Connected to coordinator.");
            };

            coordinator.onmessage = (event) => {
                const message = event.data;
                if (message.startsWith("NEW_LEADER:")) {
                    const newAddress = message.split("NEW_LEADER:")[1].trim();
                    console.log("Received new leader update:", newAddress);
                    const port = newAddress.split(":").pop();
                    console.log("Port: ", port);
                    const newLeaderAddress = `ws://127.0.0.1:${port}`;

                    if (gameSocket) {
                        gameSocket.close();
                    }

                    setServerAddress(newLeaderAddress);
                }
            };

            coordinator.onerror = (error) => {
                console.error("Coordinator WebSocket error:", error);
            };

            coordinator.onclose = () => {
                console.warn("Coordinator WebSocket closed. Reconnecting...");
                setTimeout(connectCoordinator, 3000); // Reconnect after 3 seconds
            };

            coordinatorRef.current = coordinator;
        };

        connectCoordinator();
    }, []);

    return (
        <WebSocketContext.Provider value={{ socket, isConnected }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
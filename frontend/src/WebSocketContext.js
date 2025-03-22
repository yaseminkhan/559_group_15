import React, { createContext, useContext, useState, useEffect } from "react";

const WebSocketContext = createContext(null);
let gameSocket = null;

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState("ws://localhost:8887"); // initial backend
    const [coordinatorSocket, setCoordinatorSocket] = useState(null);

    // Main game socket to backend server
    useEffect(() => {
        const connectGameSocket = () => {
            if (!gameSocket || gameSocket.readyState === WebSocket.CLOSED) {
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
                    setIsConnected(false);
                    setTimeout(connectGameSocket, 3000);
                };

                setSocket(gameSocket);
            }
        };

        connectGameSocket();
    }, [serverAddress]);

    // Separate socket to coordinator (only for NEW_LEADER messages)
    useEffect(() => {
        const coordinator = new WebSocket("ws://localhost:9999");
        coordinator.onopen = () => {
            console.log("Connected to coordinator.");
        };

        coordinator.onmessage = (event) => {
            const message = event.data;
            if (message.startsWith("NEW_LEADER:")) {
                const newAddress = message.split("NEW_LEADER:")[1].trim();
                console.log("Received new leader update:", newAddress);

                // Close existing gameSocket
                if (gameSocket) {
                    gameSocket.close();
                }

                // Update backend connection address
                setServerAddress(newAddress);
            }
        };

        coordinator.onerror = (error) => {
            console.error("Coordinator WebSocket error:", error);
        };

        coordinator.onclose = () => {
            console.warn("Coordinator WebSocket closed.");
            // Optional: Reconnect logic if needed
        };

        setCoordinatorSocket(coordinator);
    }, []);

    return (
        <WebSocketContext.Provider value={{ socket, isConnected }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
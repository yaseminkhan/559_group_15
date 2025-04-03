
import React, { createContext, useContext, useState, useEffect, useRef } from "react";

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState(null);
    const coordinatorRef = useRef(null);

    // Connect to backend server
    useEffect(() => {
        if (!serverAddress) return;

        const ws = new WebSocket(serverAddress);

        ws.onopen = () => {
            console.log(`Connected to backend: ${serverAddress}`);
            setIsConnected(true);
            setSocket(ws);

            const storedUserId = localStorage.getItem("userId");
            if (storedUserId) {
                console.log(`Reconnecting as user: ${storedUserId}`);
                ws.send(`/reconnect ${storedUserId}`);
            }
        };

        ws.onmessage = (event) => {
            const message = event.data;
            if (message.startsWith("USER_ID:")) {
                const userId = message.split(":")[1];
                console.log(`Received user ID: ${userId}`);
                localStorage.setItem("userId", userId);
            }
        };

        ws.onerror = (error) => {
            console.error("Game WebSocket error:", error);
        };

        ws.onclose = () => {
            console.warn("Game WebSocket closed.");
            setIsConnected(false);
            setSocket(null);
        };

        return () => {
            console.log("🔌 Cleaning up old socket connection.");
            ws.close();
        };
    }, [serverAddress]);

    // Connect to coordinator
    useEffect(() => {
        const connectCoordinator = () => {
            const coordinator = new WebSocket("ws://100.78.239.70:9999");

            coordinator.onopen = () => {
                console.log("Connected to coordinator.");
                coordinator.send("GET_LEADER");
            };

            coordinator.onmessage = (event) => {
                const message = event.data;
                if (message.startsWith("NEW_LEADER:")) {
                    const newAddress = message.split("NEW_LEADER:")[1].trim();
                    const port = newAddress.split(":").pop();
                    const newLeaderAddress = `ws://localhost:${port}`;

                    console.log("Received new leader update:", newLeaderAddress);

                    // Trigger socket reconnection
                    setServerAddress(newLeaderAddress);
                }
            };

            coordinator.onerror = (error) => {
                console.error("Coordinator WebSocket error:", error);
            };

            coordinator.onclose = () => {
                console.warn("Coordinator WebSocket closed. Reconnecting...");
                setTimeout(connectCoordinator, 3000);
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

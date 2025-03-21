import React, { createContext, useContext, useState, useEffect } from "react";

const WebSocketContext = createContext(null);
let ws = null;

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState("ws://localhost:8887"); 

    useEffect(() => {
        const connectWebSocket = () => {
            if (!ws || ws.readyState === WebSocket.CLOSED) {
                ws = new WebSocket(serverAddress);

                ws.onopen = () => {
                    console.log(`Connected to WebSocket at: ${serverAddress}`);
                    setIsConnected(true);
                
                    const storedUserId = localStorage.getItem("userId");
                    if (storedUserId) {
                        console.log(`Reconnecting as user: ${storedUserId}`);
                        ws.send(`/reconnect ${storedUserId}`);
                    }
                };

                ws.onmessage = (event) => {
                    const message = event.data;  

                    if (message.startsWith("/USER_ID")) {
                        const userId = message.split(" ")[1];
                        console.log(`Connected as user: ${userId}`);
                        localStorage.setItem("userId", userId);
                    } else if (message.startsWith("NEW_LEADER:")) {
                        const newLeaderAddress = message.split(":")[1].trim();
                        console.log(`New leader detected: ${newLeaderAddress}`);
                        const port = message.split(":")[1].trim();

                        // Update state instead of modifying a variable
                        setServerAddress(`ws://localhost:${port}`);

                        // Close current connection and reconnect to the new leader
                        ws.close();
                        setTimeout(connectWebSocket, 1000); // Short delay before reconnecting
                    }
                };

                ws.onerror = (error) => console.error("WebSocket Error:", error);

                ws.onclose = () => {
                    console.log("WebSocket Disconnected. Retrying in 3 seconds...");
                    setIsConnected(false);
                    setTimeout(connectWebSocket, 3000);
                };

                setSocket(ws);
            }
        };

        connectWebSocket();

        return () => {
            // Do NOT close the WebSocket when the component unmounts
        };
    }, [serverAddress]);  // Add serverAddress as a dependency

    return (
        <WebSocketContext.Provider value={{ socket, isConnected }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
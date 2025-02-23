import React, { createContext, useContext, useState, useEffect } from "react";

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8887");

        ws.onopen = () => console.log("Connected to server!");
        ws.onerror = (error) => console.error("WebSocket Error:", error);

        setSocket(ws);

        return () => ws.close(); // Cleanup on unmount
    }, []);

    return (
        <WebSocketContext.Provider value={socket}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
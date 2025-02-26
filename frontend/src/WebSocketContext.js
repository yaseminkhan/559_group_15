import React, { createContext, useContext, useState, useEffect } from "react";

const WebSocketContext = createContext(null);
let ws = null;

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);

    useEffect(() => {
        const connectWebSocket = () => {
            if (!ws || ws.readyState === WebSocket.CLOSED) {
                ws = new WebSocket("ws://localhost:8887");

                ws.onopen = () => {
                    console.log("WebSocket Connected");
                
                    const storedUserId = localStorage.getItem("userId");
                    if (storedUserId) {
                        console.log(`Reconnecting as user: ${storedUserId}`);
                        ws.send(`/reconnect ${storedUserId}`);
                    }
                };

                ws.onerror = (error) => console.error("WebSocket Error:", error);

                ws.onclose = () => {
                    console.log("WebSocket Disconnected. Retrying in 3 seconds...");
                    setTimeout(connectWebSocket, 3000);
                };

                setSocket(ws);
            }
        };

        connectWebSocket();

        return () => {
            // Do NOT close the WebSocket when the component unmounts
        };
    }, []);

    return (
        <WebSocketContext.Provider value={socket}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
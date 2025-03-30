import React, { createContext, useContext, useState, useEffect, useRef } from "react";

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState(null);
    const coordinatorRef = useRef(null);
    const queue = useRef([]);
    const wasClosedRef = useRef(false);

    const queueOrSendEvent = (prefix, eventData) => {
        const message = prefix + " " + JSON.stringify(eventData);

        const ready = socket && socket.readyState === WebSocket.OPEN;
        const closedOrClosing = !socket || socket.readyState >= WebSocket.CLOSING;

        if (!closedOrClosing && ready) {
            console.log("[Sent immediately]:", message);
            socket.send(message);
        } else {
            console.log("[Queued]:", message);
            queue.current.push(message);
        }
    };

    const handleIncomingMessage = (event) => {
        const message = event.data;
        if (message.startsWith("USER_ID:")) {
            const userId = message.split(":")[1];
            console.log(`Received user ID: ${userId}`);
            localStorage.setItem("userId", userId);
        }
    };

    const flushQueue = (ws) => {
        if (!queue.current || queue.current.length === 0) return;

        console.log("=============== FLUSHING QUEUE ===============");
        queue.current.forEach((msg) => {
            console.log("[Sending from queue]:", msg);
            ws.send(msg);
        });
        console.log("===============================================");
        queue.current = [];
    };

    useEffect(() => {
        if (!serverAddress) return;

        const ws = new WebSocket(serverAddress);

        ws.onopen = () => {
            console.log(`Connected to backend: ${serverAddress}`);
            setIsConnected(true);
            setSocket(ws);
            wasClosedRef.current = false;

            const storedUserId = localStorage.getItem("userId");
            if (storedUserId) {
                console.log(`Reconnecting as user: ${storedUserId}`);
                ws.send(`/reconnect ${storedUserId}`);
            }
            flushQueue(ws);
        };

        ws.onmessage = handleIncomingMessage;

        ws.onerror = (error) => {
            console.error("Game WebSocket error:", error);
        };

        ws.onclose = () => {
            console.warn("Game WebSocket closed.");
            setIsConnected(false);
            setSocket(null);
            wasClosedRef.current = true;
        };

        return () => {
            console.log("Cleaning up old socket connection.");
            ws.close();
        };
    }, [serverAddress]);

    useEffect(() => {
        const connectCoordinator = () => {
            const coordinator = new WebSocket("ws://localhost:9999");

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
        <WebSocketContext.Provider value={{ socket, isConnected, queueOrSendEvent, wasClosedRef }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);

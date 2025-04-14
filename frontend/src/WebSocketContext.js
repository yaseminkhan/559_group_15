
import React, { createContext, useContext, useState, useEffect, useRef } from "react";

let logicalClock = 0;

export const tickClock = () => {
    const newTime = ++logicalClock;
    console.trace(`[LAMPORT] tickClock() -> ${newTime}`);
    return newTime;
};

export const updateClock = (remoteTime) => {
    logicalClock = Math.max(logicalClock, remoteTime);
    return ++logicalClock;
};

export const getClock = () => logicalClock;

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState(null);
    const coordinatorRef = useRef(null);
    const queue = useRef([]);
    const wasClosedRef = useRef(false);

    const queueOrSendEvent = (prefix, eventData) => {
        const sequenceNumber = tickClock(); // always tick the logical clock
      
        const messageWithTimestamp = {
          ...eventData,
          sequenceNumber,
          id: localStorage.getItem("userId"),
        };

        console.log(`[LAMPORT] Preparing message with sequenceNumber: ${sequenceNumber}`);
      
        const message = prefix + " " + JSON.stringify(messageWithTimestamp);
      
        const ready = socket && socket.readyState === WebSocket.OPEN;
        const closedOrClosing = !socket || socket.readyState >= WebSocket.CLOSING;
      
        if (!closedOrClosing && ready) {
          //console.log("[Sent immediately]:", message);
          socket.send(message);
        } else {
          //console.log("[Queued]:", message);
          queue.current.push(message);
        }
    };

    const handleIncomingMessage = (event) => {
        const message = event.data;
        console.log("WebSocket message:", message);
        if (message.startsWith("USER_ID:")) {
            const userId = message.split(":")[1];
            console.log(`Received user ID: ${userId}`);
            localStorage.setItem("userId", userId);
            return;
        }
        if (message.startsWith("LAMPORT:")) {
            const remoteTime = parseInt(message.split(":")[1], 10);
            const updated = updateClock(remoteTime);
            console.log(`[LAMPORT] Received backend clock: ${remoteTime}, Updated local clock: ${updated}`);
            return;
        }
    
    };

    const flushQueue = (ws) => {
        if (!queue.current || queue.current.length === 0) return;

        //console.log("=============== FLUSHING QUEUE ===============");
        queue.current.forEach((msg) => {
            //console.log("[Sending from queue]:", msg);
            ws.send(msg);
        });
        //console.log("===============================================");
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

            // Wait until WebSocket is fully ready before flushing queue
            const waitForOpen = setInterval(() => {
                if (ws.readyState === WebSocket.OPEN) {
                    //console.log("WebSocket is open! Flushing queue...");
                    flushQueue(ws);
                    clearInterval(waitForOpen);
                } else {
                    console.log("Waiting for WebSocket to stabilize...");
                }
            }, 200); // Check every 200ms
        };

        ws.addEventListener("message", handleIncomingMessage);

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
            //console.log("Cleaning up old socket connection.");
            ws.close();
        };
    }, [serverAddress]);

    useEffect(() => {
        const connectCoordinator = () => {
            const coordinator = new WebSocket("ws://100.76.248.111:9999");

            coordinator.onopen = () => {
                //console.log("Connected to coordinator.");
                coordinator.send("GET_LEADER");
            };

            coordinator.onmessage = (event) => {
                const message = event.data;
                if (message.startsWith("NEW_LEADER:")) {
                    const newAddress = message.split("NEW_LEADER:")[1].trim();
                    console.log("Received new leader update:", newAddress);
                    setServerAddress(newAddress);
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

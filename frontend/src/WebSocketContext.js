import React, { createContext, useContext, useState, useEffect, useRef } from "react";

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState(null);
    const coordinatorRef = useRef(null);
    const lamportClock = useRef(0);
    const queue = useRef([]);

    const getLamportTimestamp = () => ++lamportClock.current; // increment on send 

    const updateLamportClock = (receivedTimestamp) => lamportClock.current = Math.max(lamportClock.current, receivedTimestamp) + 1;

    // every message sent through here gets a timestamp and is buffered if the socket disconnects 
    const queueOrSendEvent = (prefix, eventData) => {
        const sequenceNo = getLamportTimestamp();
        const event = { ...eventData, sequenceNo };
        const message = prefix + " " + JSON.stringify(event);
        if (isConnected && socket) 
            socket.send(message);
        else
            queue.current.push(message);
    };

    // String.split, but like better.
    const split = (str, delim, maxSplit) => {
        if (!str.includes(delim))
            throw new Error("Delimiter is not in string.");
        if (maxSplit === 0) return str;
        const i = str.indexOf(delim);
        const [left, right] = [str.slice(0, i), str.slice(i+1)];
        return (maxSplit === 1)
                    ? [left, right]
                    : [left, ...split(right, delim, maxSplit-1)];
    }

    // Sends current stored messages (clear buffer when server reconnects)
    const flushQueue = (ws) => {
        if (queue.current === undefined || queue.current.length === 0) return;  // Array is empty or undefined.

        const arr = [...queue.current]
            .map(s => split(s, " ", 2)) 
            .map(([prefix, gameCode, msgStr]) => [prefix, gameCode, JSON.parse(msgStr)])
            .sort(([,, a], [,, b]) => a.sequenceNo - b.sequenceNo)
            .map(([prefix, gameCode, msg]) => prefix + " " + gameCode + " " + JSON.stringify(msg));

        console.log(
            "===============PRINTING QUEUE================",
            arr,
            "=============================================",
        )

        arr.forEach(msg => ws.send(msg));
        queue.current = [];
        
        console.log("DONE FLUSHING QUEUE.")
    }

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
            flushQueue(ws);
        };

        ws.onmessage = (event) => {
            const message = event.data;
            if (message.startsWith("USER_ID:")) {
                const userId = message.split(":")[1];
                console.log(`Received user ID: ${userId}`);
                localStorage.setItem("userId", userId);
            }
        }

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
        <WebSocketContext.Provider value={{ socket, isConnected, queueOrSendEvent }}>
            {children}
        </WebSocketContext.Provider>
    );
};

export const useWebSocket = () => useContext(WebSocketContext);
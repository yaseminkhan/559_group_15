import React, { createContext, useContext, useState, useEffect, useRef } from "react";

const WebSocketContext = createContext(null);

export const WebSocketProvider = ({ children }) => {
    const [socket, setSocket] = useState(null);
    const [isConnected, setIsConnected] = useState(false);
    const [serverAddress, setServerAddress] = useState(null);
    const coordinatorRef = useRef(null);
    const lamportClock = useRef(0);
    const queue = useRef([]);

    const getLamportTimestamp = () => ++lamportClock.current;

    const updateLamportClock = (receivedTimestamp) => lamportClock.current = Math.max(lamportClock.current, receivedTimestamp) + 1;

    const queueOrSendEvent = (prefix, eventData) => {
        const sequenceNo = getLamportTimestamp();
        const event = { ...eventData, sequenceNo };
        const message = prefix + " " + JSON.stringify(event);
        if (isConnected && socket?.readyState === WebSocket.OPEN) 
            socket.send(message);
        else
            queue.current.push(message);
    };

    // String.split, but like better.
    const split = (str, delim, maxSplit) => {
        if (maxSplit === 0) return str;
        const i = str.indexOf(delim);
        const [left, right] = [str.slice(0, i), str.slice(i+1)];
        return (maxSplit === 1)
                    ? [left, right]
                    : [left, ...split(right, delim, maxSplit-1)];
    }

    const handleIncomingMessage = (event) => {
        const message = event.data;
        if (message.startsWith("USER_ID:")) {
            const userId = message.split(":")[1];
            console.log(`Received user ID: ${userId}`);
            localStorage.setItem("userId", userId);
        }
    }

    const flushQueue = () => {
        // [...queue.current]
        //     .sort((a, b) => a.sequenceNo - b.sequenceNo)
        //     .forEach(e => socket.send(JSON.stringify(e)));
        if (queue.current === undefined || queue.current.length === 0) return;  // Array is empty or undefined.

        const arr = [...queue.current]
            .map(s => split(s, " ", 1))
            .sort(([p1, a], [p2, b]) => a.sequenceNo - b.sequenceNo)
            .map(([prefix, message]) => prefix + " " + JSON.stringify(message));

        console.log(
            "===============PRINTING QUEUE================",
            arr,
            "=============================================",
        )

        // Debugging.
        if (!socket) {
            console.error("SOCKET IS NULL.");
        }

        arr.forEach(socket.send);

        queue.current = [];
    }

    // Connect to backend server
    useEffect(() => {
        if (!serverAddress) return;

        const ws = new WebSocket(serverAddress);

        ws.onopen = () => {
            console.log(`Connected to backend: ${serverAddress}`);
            setIsConnected(true);
            setSocket(ws);
            flushQueue();
            
            const storedUserId = localStorage.getItem("userId");
            if (storedUserId) {
                console.log(`Reconnecting as user: ${storedUserId}`);
                ws.send(`/reconnect ${storedUserId}`);
            }

        };

        ws.onmessage = handleIncomingMessage;

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
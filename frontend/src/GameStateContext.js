import React, { createContext, useContext, useState, useEffect } from "react";
import { useWebSocket } from "./WebSocketContext";

export const GameStateContext = createContext();

export const GameStateProvider = ({ children }) => {
    const socket = useWebSocket();

    const [userId, setUserId] = useState(localStorage.getItem("userId") || null);
    const [gameCode, setGameCode] = useState(localStorage.getItem("gameCode") || null);
    const [players, setPlayers] = useState([]);
    const [timeLeft, setTimeLeft] = useState(60);
    const [canvasHistory, setCanvasHistory] = useState([]);
    const [chatMessages, setChatMessages] = useState([]);

    useEffect(() => {
        if (!socket) return;
        if (userId && socket.readyState === WebSocket.OPEN) {
            socket.send(`/reconnect ${userId}`);
        }
    }, [socket, userId]);

    // Listen messages to update all states
    useEffect(() => {
        if (!socket) return;

        const handleMessage = (event) => {
            const data = event.data;
            if (data.startsWith("GAME_PLAYERS")) {
                try {
                    const message = JSON.parse(data);
                    if (message.type === "GAME_PLAYERS") {
                        setPlayers(JSON.parse(message.data));
                    }
                } catch (error) {
                    console.error("ERROR : Parsing GAME_PLAYERS message", error);
                }
            }
        
            // Timer
            if (data.startsWith("TIMER_UPDATE")) {
                const newTime = parseInt(data.split(": ")[1], 10);
                setTimeLeft(newTime);
            }

            // Canvas History
            if (data.startsWith("CANVAS_HISTORY")) {

            }

            // Chat History
            if (data.startsWith("CHAT_HISTORY")) {
                const i = data.indexOf(" ");
                const chatJson = data.slice(i + 1);
                try {
                    const chat = JSON.parse(chatJson);
                    setChatMessages(chat);
                } catch (error) {
                    console.error("ERROR : parsing CHAT_HISTORY", error);
                }
            }
        };

        socket.addEventListener("message", handleMessage);
        return () => socket.removeEventListener("message", handleMessage);
    }, [socket]);
};

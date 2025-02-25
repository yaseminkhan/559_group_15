import React, { useState, useEffect } from "react";
import "../styles/Header.css";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCrown } from "@fortawesome/free-solid-svg-icons";
import { useWebSocket } from "../WebSocketContext"; // Import WebSocket context

const Header = ({ isChoosingWord, gameCode }) => {
    const [players, setPlayers] = useState([]); // Store players from backend
    const [timeLeft, setTimeLeft] = useState(60);
    const socket = useWebSocket();

    useEffect(() => {
        if (!socket) return;

        // Request player list when component mounts
        console.log(`Requesting players for game: ${gameCode}`);
        socket.send(`/getgame ${gameCode}`);

        const handleMessage = (event) => {
            console.log("WebSocket message:", event.data);

            try {
                const message = JSON.parse(event.data);

                if (message.type === "GAME_PLAYERS") {
                    console.log("Updating players list...");
                    setPlayers(JSON.parse(message.data)); // Update players
                }
            } catch (error) {
                console.error("Error parsing WebSocket message:", error);
            }
        };

        socket.addEventListener("message", handleMessage);

        return () => {
            socket.removeEventListener("message", handleMessage);
        };
    }, [socket, gameCode]);

    useEffect(() => {
        if (timeLeft === 0 || isChoosingWord) return; 

        const timer = setInterval(() => {
            setTimeLeft((prevTime) => prevTime - 1);
        }, 1000);

        return () => clearInterval(timer);
    }, [timeLeft, isChoosingWord]);

    // Determine the highest score
    const highestScore = players.length > 0 ? Math.max(...players.map(player => player.score || 0)) : 0;

    return (
        <div className="header">
            <div className="clock">{timeLeft}</div>
            <div className="player-container">
                {players.map((player, index) => (
                    <div key={index} className="player">
                        {player.score === highestScore && player.score != 0 && <FontAwesomeIcon icon={faCrown} className="crown" />}
                        <span className="avatar">{player.icon || "❓"}</span>
                        <span className="name">{player.username || "Unknown"}</span>
                        <span className="score">{player.score !== undefined ? `${player.score} pts` : "No score"}</span>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default Header;
import React, { useState, useEffect } from "react";
import "../styles/Header.css";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faCrown } from "@fortawesome/free-solid-svg-icons";
import { useWebSocket } from "../WebSocketContext"; // Import WebSocket context

const Header = ({ isChoosingWord, gameCode }) => {
    const [players, setPlayers] = useState([]); // Store players from backend
    const [timeLeft, setTimeLeft] = useState(60);
    const { socket, isConnected } = useWebSocket() || {}; // Get WebSocket context

    
    useEffect(() => {
        // if not socket open, return
        if (!socket || !isConnected) return;
        socket.send(`/getgame`);
        // Routine check getgame response
        const handleMessage = (event) => {
            console.log("WebSocket message:", event.data);
            try {
                const message = JSON.parse(event.data);

                if (message.type === "GAME_PLAYERS") {
                    console.log("Updating players list...");
                    setPlayers(JSON.parse(message.data)); // Update players
                }
            } catch (error) {
                //console.error("Error parsing WebSocket message:", error);
            }
        };

        socket.addEventListener("message", handleMessage);

        return () => {
            socket.removeEventListener("message", handleMessage);
        };
    }, [socket, gameCode]);


    useEffect(() => {
        const interval = 200;  // 200ms polling interval.
        const getPlayers = () => {
            if (socket && isConnected) {
                console.log("requiesting players for game: ", gameCode);
                socket.send(`/getgame ${gameCode}`);
            }
        }
        const intervalId = setInterval(getPlayers, interval);
        return () => clearInterval(intervalId);
    }, [players, gameCode])

    // timer use effect
    useEffect(() => {
      if (!socket) return;
  
      const handleMessage = (event) => {
          if (event.data.startsWith("TIMER_UPDATE:")) {
              const newTime = parseInt(event.data.split(": ")[1]);
              console.log("Updating timer in header to:", newTime);
              setTimeLeft(newTime);
          }
      };
  
      socket.addEventListener("message", handleMessage);
      return () => {
          socket.removeEventListener("message", handleMessage);
      };
    }, [socket]);

    // Determine the highest score
    const highestScore = players.length > 0 ? Math.max(...players.map(player => player.score || 0)) : 0;


    // Player icon, username, score needs to be saved somewhere
    return (
        <div className="header">
            <div className="clock">{timeLeft}</div>
            <div className="player-container">
                {players.map((player, index) => (
                    <div key={index} className="player">
                        {player.score === highestScore && player.score !== 0 && <FontAwesomeIcon icon={faCrown} className="crown"/>}
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
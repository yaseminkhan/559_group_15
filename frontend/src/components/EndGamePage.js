import React, { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext";
import "../styles/EndGamePage.css";
import "../styles/WelcomePage.css";

const EndGamePage = () => {
    const navigate = useNavigate();
    const { socket, isConnected, queueOrSendEvent } = useWebSocket() || {}; // Get WebSocket context
    const [players, setPlayers] = useState([]);
    const location = useLocation();
    const gameCode = location.state?.gameCode || localStorage.getItem("gameCode");

    useEffect(() => {
        if (!socket || !isConnected) return;
        
        console.log(`Requesting players for game: ${gameCode}`);
        socket.send(`/getgame ${gameCode}`);
        
        const handleMessage = (event) => {
            console.log("WebSocket message:", event.data);
            
            try {
                const message = JSON.parse(event.data);

                if (message.type === "GAME_PLAYERS") {
                    console.log("Received final player scores.");                    
                    setPlayers(JSON.parse(message.data));
                }
            } catch (error) {
                console.error("Error parsing WebSocket message:", error);
            }
        };
        
        socket.addEventListener("message", handleMessage);
        
        return () => {
            socket.removeEventListener("message", handleMessage);
        };
    }, [socket]);

    // Sort players by score (highest to lowest)
    const sortedPlayers = [...players].sort((a, b) => b.score - a.score);

    const handleEndGame = () => {
        if (socket && isConnected) {
            socket.send(`/endgame ${gameCode}`);

            // clear localstorage
            localStorage.setItem("isDrawer", null);
            localStorage.setItem("isChoosingWord", null);
            localStorage.setItem("wordToDraw", null);

            navigate('/');
        } else {
            alert("WebSocket is not connected!");
        }
    }

    return (
        <div className="endgame_container">           
            <div className="endgame_box">
                <h1 className="end_logo">InkBlink</h1> 
                <h1 className="endgame_title">Game Over!</h1>
                
                <div className="endgame_list">
                    {sortedPlayers.map((player, index) => (
                        <div key={index} className="endgame_player">
                            <span className="endgame_rank">#{index + 1}</span>
                            <span className="endgame_avatar">{player.icon}</span>
                            <span className="endgame_name">{player.username}</span>
                            <span className="endgame_score">{player.score} pts</span>
                        </div>
                    ))}
                </div>

                <button className="endgame_btn" onClick={handleEndGame}>Back to Home</button>
            </div>
        </div>
    );
};

export default EndGamePage;

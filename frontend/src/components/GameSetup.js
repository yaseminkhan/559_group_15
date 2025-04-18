import React, { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GameSetup.css";
import userIcon from "./assets/setupPage/user.png";
import crayons from "./assets/setupPage/crayons.png";
import hourglass from "./assets/setupPage/hourglass.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
    const { gameCode } = useParams(); 
    const navigate = useNavigate();
    const [players, setPlayers] = useState([]);
    const [isHost, setIsHost] = useState(false);
    const { socket, isConnected, queueOrSendEvent } = useWebSocket() || {}; // Get WebSocket context


    const handleStartGame = () => {
        if (socket && isHost) {
            if (players.length < 2) {
                alert("At least 2 players are required to start the game.");
                return;
            }
            const userId = localStorage.getItem("userId");
            socket.send(`/startgame ${gameCode} ${userId}`);
        }
    };

    useEffect(() => {
    
        if (!socket) {
            console.log("WebSocket is null. Exiting use Effect.");
            return;
        }

        const handleMessage = (event) => {
        
            try {
                if (typeof event.data === "string" && event.data.startsWith("GAME_STARTED: ")) {
                    const firstDrawer = event.data.split(" ")[1];
                    console.log("Game is starting...");
                    const userId = localStorage.getItem("userId");
        
                    if (firstDrawer === userId) {
                        localStorage.setItem("isDrawer", true);
                        navigate(`/wordselection/${gameCode}`); 
                    } else {
                        console.log(`Navigating to game page. Waiting for drawer to choose a word.`);
                        localStorage.setItem("isDrawer", false);
                        localStorage.setItem("isChoosingWord", true);
                        navigate(`/game/${gameCode}`);
                    }
                    return;
                }
                
        
                // Check if event.data is JSON before parsing
                let message;
                try {
                    message = JSON.parse(event.data);
                } catch (jsonError) {
                    console.warn("Received non-JSON WebSocket message:", event.data);
                    return; // Skip non-JSON messages
                }
        
                if (message.type === "GAME_PLAYERS") {
                    console.log("GAME_PLAYERS event received. Updating players...");
                    try {
                        const gamePlayers = JSON.parse(message.data);
                        setPlayers(gamePlayers);
                        console.log("Updated Players List:");
                        gamePlayers.forEach((player) => {
                            console.log(` - ${player.username} (ID: ${player.id})`);
                        });
        
                        const userId = localStorage.getItem("userId");
                        setIsHost(gamePlayers.length > 0 && gamePlayers[0].id === userId);
                    } catch (parseError) {
                        console.error("Error parsing GAME_PLAYERS data:", parseError);
                    }
                }
            } catch (error) {
                console.error("Error handling WebSocket message:", error);
            }
        };
        
    
        const attachWebSocketListeners = () => {
            console.log("Attaching WebSocket message listener...");
    
            const userId = localStorage.getItem("userId");
    
            if (userId && !socket.hasReconnected) {
                console.log("Attempting to reconnect...");
                socket.send(`/reconnect ${userId}`);
                socket.hasReconnected = true;
            }
    
            console.log(`Requesting player list for game: ${gameCode}`);
            socket.send(`/getgame ${gameCode}`);
    
            socket.addEventListener("message", handleMessage);
        };
    
        if (socket.readyState === WebSocket.OPEN) {
            console.log("WebSocket is already open. Proceeding...");
            attachWebSocketListeners();
        } else {
            console.log("WebSocket is not open. Waiting for connection...");
            socket.onopen = () => {
                console.log("WebSocket just opened. Attaching event listeners...");
                attachWebSocketListeners();
            };
        }
    
        return () => {
            socket.removeEventListener("message", handleMessage);
        };
    }, [socket, gameCode, navigate]);

    return (
        <div className="setup_container">
            <img src={left_bkg} alt="Left Top" className="left-top-image" />
            <img src={right_bkg} alt="Right Top" className="right-top-image" />
            <div className="setup_logo"><h1>Game Lobby</h1></div>
            <hr className="underline"/>
            <div className="setup_content_container">
                <div className="setup_join_box">
                    <div className="setup_join_title">JOIN CODE:</div>
                    <div className="setup_join_code">{gameCode}</div>
                </div>
                <div className="setup_image">
                    <img src={hourglass} alt="hourglass" className="setup_image hourglass" />
                    <img src={crayons} alt="crayons" className="setup_image crayons" />
                </div>
                <div className="setup_players_box">
                    <div className="setup_player_title">
                        <img src={userIcon} alt="Players" className="setup-players" />
                        <div className="setup_join_title">Players</div>
                    </div>
                    <div className="setup_player_list">
                        {players.map((player, index) => (
                            <div key={index} className="setup_player_card">
                                <span className="avatar">{player.icon}</span>
                                <span className="name">{player.username}</span>
                            </div>
                        ))}
                    </div>
                </div>

                {/* Show the Start Game button only if the user is the host */}
                {isHost && (
                    <div className="setup_btn_group">
                        <button className="setup_btn" onClick={handleStartGame}>Start Game</button>
                    </div>
                )}
            </div>
        </div>
    );
};

export default GameSetup;
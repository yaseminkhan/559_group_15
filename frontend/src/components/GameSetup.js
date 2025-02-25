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
    const socket = useWebSocket(); 

    const handleStartGame = () => {
        if (socket && isHost) {
            const userId = localStorage.getItem("userId");
            socket.send(`/startgame ${gameCode} ${userId}`);
        }
    };

    useEffect(() => {
        console.log("GameSetup useEffect running...");
    
        if (!socket) {
            console.log("WebSocket is null. Exiting useEffect.");
            return;
        }

        const handleMessage = (event) => {
            console.log("\n========== WebSocket MESSAGE RECEIVED ==========");
            console.log("Raw Data:", event.data);
        
            try {
                // If the message is a known string-based format, handle it first
                if (typeof event.data === "string" && event.data.startsWith("GAME_STARTED: ")) {
                    const firstDrawer = event.data.split(" ")[1];
                    console.log("Game is starting...");
                    const userId = localStorage.getItem("userId");

                    // Find the first drawer's details from the gamePlayers list
                    // const firstDrawerInfo = gamePlayers.find(player => player.id === firstDrawer);
        
                    if (firstDrawer === userId) {
                        console.log("You are the first drawer. Navigating to word selection page.");
                        navigate(`/wordselection`);
                        // navigate(`/wordselection`, { state: { currentDrawer: firstDrawerInfo } });
                    } else {
                        console.log(`Navigating to game page. Waiting for drawer to choose a word.`);
                        navigate(`/game`, { state: { choosingWord: true, isDrawer: false } });
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
import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/GameSetup.css";
import userIcon from "./assets/setupPage/user.png";
import crayons from "./assets/setupPage/crayons.png";
import hourglass from "./assets/setupPage/hourglass.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
    const { gameCode } = useParams(); 
    const [players, setPlayers] = useState([]);
    const socket = useWebSocket(); 

    useEffect(() => {
        if (!socket) return;
    
        console.log("Connected to server!");
        socket.send(`/getgame ${gameCode}`); // Initial request to get players
    
        const handleMessage = (event) => {
            console.log("Received:", event.data);
            
            try {
                const message = JSON.parse(event.data);
                if (message.type === "GAME_PLAYERS") {
                    const gamePlayers = JSON.parse(message.data); // Parse player list
                    setPlayers(gamePlayers);
                }
            } catch (error) {
                console.error("Error parsing JSON:", error);
            }
        };
    
        socket.addEventListener("message", handleMessage);
    
        return () => {
            socket.removeEventListener("message", handleMessage);
        };
    }, [socket, gameCode]);
    

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
            </div>
        </div>
    );
};

export default GameSetup;
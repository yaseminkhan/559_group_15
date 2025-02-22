import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import "../styles/GameSetup.css";
import userIcon from "./assets/setupPage/user.png";
import crayons from "./assets/setupPage/crayons.png";
import hourglass from "./assets/setupPage/hourglass.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
    const { gameCode } = useParams(); // Get game code from URL
    const [players, setPlayers] = useState([]);
    const [socket, setSocket] = useState(null);

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8887");

        ws.onopen = () => {
            console.log("Connected to server!");
            ws.send(`/getgame ${gameCode}`); // Request game info
        };

        ws.onmessage = (event) => {
            console.log("Received:", event.data);
        
            try {
                if (event.data.startsWith("GAME_PLAYERS:")) {
                    const jsonString = event.data.substring(13);
                    const gamePlayers = JSON.parse(jsonString);
                    console.log("Parsed players:", gamePlayers);
                    setPlayers(gamePlayers); 
                } else {
                    console.log("Other message received:", event.data);
                }
            } catch (error) {
                console.error("Error parsing JSON:", error);
            }
        };

        setSocket(ws);

        return () => {
            ws.close();
        };
    }, [gameCode]);

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
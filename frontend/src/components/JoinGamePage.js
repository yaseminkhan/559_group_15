import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useWebSocket } from "../WebSocketContext"; 
import "../styles/WelcomePage.css";
import palette from "./assets/palette.png";
import brush from "./assets/brush.png";
import trophy from "./assets/trophy.png";
import friends from "./assets/friends.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const JoinGamePage = () => {
    const navigate = useNavigate();
    const [gameCode, setGameCode] = useState("");
    const [message, setMessage] = useState(""); 
    const socket = useWebSocket(); 

    useEffect(() => {
        if (!socket) return; 

        socket.onopen = () => console.log("Connected to server!");

        socket.onmessage = (event) => {
            console.log("Received:", event.data);

            if (event.data.startsWith("JOIN_SUCCESS:")) {
              const joinedGameCode = event.data.split(":")[1]; 
              localStorage.setItem("gameCode", joinedGameCode);
              navigate(`/setup/${joinedGameCode}`);
            } else if (event.data.startsWith("ERROR: Game not found.")) {
                setMessage("Invalid game code. Please try again.");
            } else if (event.data.startsWith("ERROR: User not found.")) {
                setMessage("Invalid user. Please try again.");
            } else if (event.data.startsWith("ERROR: Game is full.")) {
                alert("Game is full! Returning to the welcome page.");
                navigate("/");
            }
        };

        socket.onerror = (error) => console.error("WebSocket Error:", error);

        return () => {
            socket.onmessage = null;
            socket.onerror = null;
        };
    }, [socket, navigate]);

    const handleJoinGame = () => {
        if (!gameCode.trim()) {
            alert("Please enter a valid game code!");
            return;
        }
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(`/join-game ${gameCode}`);
        } else {
            alert("WebSocket is not connected!");
        }
    };
  
    return (
        <div className="wel_container">
            <h1 className="wel_logo">InkBlink</h1>
            <div className="wel_icons">
                <img src={palette} alt="Palette" className="wel_icon" />
                <img src={brush} alt="Brush" className="wel_icon" />
                <img src={friends} alt="Friends" className="wel_icon" />
                <img src={trophy} alt="Trophy" className="wel_icon" />
            </div>
            <h2 className="wel_subtitle">Multiplayer Game</h2>
            <input 
                type="text" 
                placeholder="Enter Game Code..." 
                className="wel_input-box" 
                value={gameCode}
                onChange={(e) => setGameCode(e.target.value)}
            />
            <div className="wel_button-group">
                <button className="wel_btn wel_join-btn" onClick={handleJoinGame}>Join</button>
                <button className="wel_btn wel_invite-btn" onClick={() => navigate("/")}>Go Back</button>
            </div>
            {message && <p className="game_message">{message}</p>}
            <img src={left_bkg} alt="Left Bottom" className="wel_left-bottom-image" />
            <img src={right_bkg} alt="Right Bottom" className="wel_right-bottom-image" />
        </div>
    );
};

export default JoinGamePage;
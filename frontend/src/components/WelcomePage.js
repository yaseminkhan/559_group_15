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

const WelcomePage = () => {
    const [username, setUsername] = useState("");
    const [userId, setUserId] = useState(null);
    const [gameCode, setGameCode] = useState(null);
    const socket = useWebSocket(); 
    const navigate = useNavigate();

    useEffect(() => {
        if (!socket) return;
        
        socket.onmessage = (event) => {
            console.log("Received:", event.data);

            if (event.data.startsWith("USER_ID:")) {
                const receivedId = event.data.split(":")[1];
                setUserId(receivedId);
                localStorage.setItem("userId", receivedId);
            } else if (event.data.startsWith("GAME_CREATED:")) {
                const newGameCode = event.data.split(":")[1];
                setGameCode(newGameCode);
                localStorage.setItem("gameCode", newGameCode);
            }
        };

        return () => {
            socket.onmessage = null; 
        };
    }, [socket]);

    useEffect(() => {
        if (gameCode) {
            console.log("Navigating to:", `/setup/${gameCode}`);
            navigate(`/setup/${gameCode}`);
        }
    }, [gameCode, navigate]);

    const handleCreateGame = () => {
        if (!username.trim()) {
            alert("Please enter a username!");
            return;
        }

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(`/setname ${username}`);
            setTimeout(() => {
                socket.send("/creategame");
            }, 500);
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
                placeholder="Enter your username..."
                className="wel_input-box"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
            />
            <div className="wel_button-group">
                <button className="wel_btn wel_invite-btn">Invite Friends</button>
                <button className="wel_btn wel_join-btn" onClick={handleCreateGame}>
                    Create Game
                </button>
            </div>
            <img src={left_bkg} alt="Left Bottom" className="wel_left-bottom-image" />
            <img src={right_bkg} alt="Right Bottom" className="wel_right-bottom-image" />
        </div>
    );
};

export default WelcomePage;
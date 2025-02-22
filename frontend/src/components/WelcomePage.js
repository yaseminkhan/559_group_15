import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import "../styles/WelcomePage.css"; 
import palette from './assets/palette.png';
import brush from './assets/brush.png';
import trophy from './assets/trophy.png';
import friends from './assets/friends.png';
import right_bkg from './assets/right_bkg.png';
import left_bkg from './assets/left_bkg.png';

const WelcomePage = () => {
    const [username, setUsername] = useState("");  
    const [userId, setUserId] = useState(null);  
    const [socket, setSocket] = useState(null);
    const [gameCode, setGameCode] = useState(null);  // ✅ New state for game code
    const navigate = useNavigate();

    useEffect(() => {
        const ws = new WebSocket("ws://localhost:8887");

        ws.onopen = () => console.log("Connected to server!");

        ws.onmessage = (event) => {
            console.log("Received:", event.data);

            if (event.data.startsWith("USER_ID:")) {
                const receivedId = event.data.split(":")[1];
                setUserId(receivedId);
                localStorage.setItem("userId", receivedId);
                console.log("Assigned User ID:", receivedId);
            } else if (event.data.startsWith("GAME_CREATED:")) {
                const newGameCode = event.data.split(":")[1];
                console.log("Game created with code:", newGameCode);

                setGameCode(newGameCode);  // ✅ Store in state
                localStorage.setItem("gameCode", newGameCode);
            } else {
                console.log("Non-JSON message:", event.data);
            }
        };

        ws.onerror = (error) => console.error("WebSocket Error:", error);
        setSocket(ws);

        return () => {
            ws.close();
        };
    }, []);

    // ✅ This useEffect ensures navigation happens AFTER state is updated
    useEffect(() => {
        if (gameCode) {
            console.log("Navigating to:", `/setup/${gameCode}`);
            navigate(`/setup/${gameCode}`);
        }
    }, [gameCode, navigate]);  // ✅ Triggers when gameCode updates

    const handleCreateGame = () => {
        if (!username.trim()) {
            alert("Please enter a username!");
            return;
        }

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(`/setname ${username}`);  
            console.log(`Sent username: ${username}`);

            setTimeout(() => {
                socket.send("/creategame");  // ✅ Create the game after setting username
                console.log("Requested to create a game.");
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
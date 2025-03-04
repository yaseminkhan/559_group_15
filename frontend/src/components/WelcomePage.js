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
    const [charLimitError, setCharLimitError] = useState("") // State for the error message of having too many char in username
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
                console.log("Assigned User ID:", receivedId);
            } else if (event.data.startsWith("GAME_CREATED:")) {
                const newGameCode = event.data.split(":")[1];
                setGameCode(newGameCode);
                localStorage.setItem("gameCode", newGameCode);
                console.log("Game created with code:", newGameCode);
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

    const handleJoinGame = () => {
        if (!username.trim()) {
            alert("Please enter a username!");
            return;
        }

        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(`/setname ${username}`);  
            console.log(`Sent username: ${username}`);

            setTimeout(() => {
                navigate("/join-game");
                //socket.send("/join-game");  
                console.log("Requested to join a game.");
            }, 500);
        } else {
            alert("WebSocket is not connected!");
        }
    };

    const handleUsername = (e) => {
        const input = e.target.value;
        if (input.length <= 15) {
            setUsername(input);
        }
        else {
            setCharLimitError("Username must be 1-15 characters.");
            setTimeout(() => setCharLimitError(""), 3000); //Pop up disappears after 3 seconds
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
                onChange={handleUsername}
                // maxLength={15} //Enforcing character limit at HTML level
            />
            {/* Vanishing Error Message */}
            {charLimitError && <div className="wel_error-popup">{charLimitError}</div>}
            

            <div className="wel_button-group">
                <button className="wel_btn wel_invite-btn" onClick={handleJoinGame}>
                    Join a Game
                </button>
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
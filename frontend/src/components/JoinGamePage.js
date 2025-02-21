import React, { useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import "../styles/WelcomePage.css";
import palette from "./assets/palette.png";
import brush from "./assets/brush.png";
import trophy from "./assets/trophy.png";
import friends from "./assets/friends.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const JoinGamePage = () => {
    const navigate = useNavigate();
    const location = useLocation();
    const username = location.state?.username || "Player"; 
    const [gameCode, setGameCode] = useState(""); // State for game code

    const handleJoinGame = () => {
      navigate("/game", { state: { username, gameCode } });
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
      <h2 className="wel_subtitle">Welcome {username}</h2>
      <input 
        type="text" 
        placeholder="Enter Game Code..." 
        className="wel_input-box" 
        value={gameCode}
        onChange={(e) => setGameCode(e.target.value)}
      />
      <div className="wel_button-group">
        <button 
          className="wel_btn wel_join-btn" 
          onClick={handleJoinGame}
          disabled={!gameCode.trim()}
        >
          Join
        </button>
        <button className="wel_btn wel_invite-btn" onClick={() => navigate("/")}>Go Back</button>
      </div>
      <img src={left_bkg} alt="Left Bottom" className="wel_left-bottom-image" />
      <img src={right_bkg} alt="Right Bottom" className="wel_right-bottom-image" />
    </div>
  );
};

export default JoinGamePage;
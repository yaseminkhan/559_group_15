// src/components/JoinGamePage.js
import React from "react";
import { useNavigate } from "react-router-dom";
import "../styles/JoinGamePage.css";
import palette from "./assets/palette.png";
import brush from "./assets/brush.png";
import trophy from "./assets/trophy.png";
import friends from "./assets/friends.png";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const JoinGamePage = () => {
    const navigate = useNavigate();
    
  return (
    <div className="container">
      <h1 className="logo">InkBlink</h1>
      <div className="icons">
        <img src={palette} alt="Palette" className="icon" />
        <img src={brush} alt="Brush" className="icon" />
        <img src={friends} alt="Friends" className="icon" />
        <img src={trophy} alt="Trophy" className="icon" />
      </div>
      <h2 className="subtitle">Multiplayer Game</h2>
      <input type="text" placeholder="Enter Game Code..." className="input-box" />
      <div className="button-group">
        <button className="btn join-btn">Join</button>
        <button className="btn go-back-btn" onClick={() => navigate("/go-back")}>Go Back</button>
      </div>
      <img src={left_bkg} alt="Left Bottom" className="left-bottom-image" />
      <img src={right_bkg} alt="Right Bottom" className="right-bottom-image" />
    </div>
  );
};

export default JoinGamePage;
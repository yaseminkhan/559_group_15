import React from "react";
import { useNavigate } from "react-router-dom";
import "../styles/WelcomePage.css"; 
import palette from './assets/palette.png';
import brush from './assets/brush.png';
import trophy from './assets/trophy.png';
import friends from './assets/friends.png';
import right_bkg from './assets/right_bkg.png';
import left_bkg from './assets/left_bkg.png';

const WelcomePage = () => {
  const navigate = useNavigate();
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
      <input type="text" placeholder="Enter your username..." className="wel_input-box" />
      <div className="wel_button-group">
        <button className="wel_btn wel_invite-btn">Invite Friends</button>
        <button className="wel_btn wel_join-btn" onClick={() => navigate("/join-game")}>Join a Game</button>
      </div>
      {/* Left and Right Bottom Images */}
      <img src={left_bkg} alt="Left Bottom" className="wel_left-bottom-image" />
      <img src={right_bkg} alt="Right Bottom" className="wel_right-bottom-image" />
    </div>
  );
};

export default WelcomePage;
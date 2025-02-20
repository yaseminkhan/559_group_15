import React from "react";
import "../styles/WelcomePage.css"; 
import palette from './assets/palette.png';
import brush from './assets/brush.png';
import trophy from './assets/trophy.png';
import friends from './assets/friends.png';
import right_bkg from './assets/right_bkg.png';
import left_bkg from './assets/left_bkg.png';

const WelcomePage = () => {
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
      <input type="text" placeholder="Enter your username..." className="input-box" />
      <div className="button-group">
        <button className="btn invite-btn">Invite Friends</button>
        <button className="btn join-btn">Join a Game</button>
      </div>
      {/* Left and Right Bottom Images */}
      <img src={left_bkg} alt="Left Bottom" className="left-bottom-image" />
      <img src={right_bkg} alt="Right Bottom" className="right-bottom-image" />
    </div>
  );
};

export default WelcomePage;

import React, {useState} from "react";
import "../styles/WordSelection.css";
import userIcon from "./assets//setupPage/user.png"
import clockIcon from "./assets//setupPage/clock.png"
import roundsIcon from "./assets//setupPage/rounds.png"
import gameModeIcon from "./assets//setupPage/gamemode.png"
import avatar from "./assets/avatar.png"
import inviteIcon from "./assets/setupPage/invite.png"
import starIcon from "./assets/wordSelectionPage/star.png"
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
  const [theme, setTheme] = useState("Animals");
  const words = {
    Animals: ["Cat", "Dog", "Horse", "Hamster"],
    Furniture: ["Table", "Chair", "Sofa", "Bed"],
    Food: ["Pizza", "Burger", "Apple", "Cake"],
  };
  return (
    <div className="container">
      <h1 className="logo">InkBlink</h1>
      <hr class="underline"/>
      <h2 className="setup-heading">Your Turn...</h2>
      <div className="content-box">
        <div className="user-card">
          <img src={avatar} alt="User-Avatar" className="user-avatar"/>
          <div>
            <p className="user-name">Jerry</p>
            <p className="user-points">0 points</p>
          </div>
        </div>
        <div className="setup-box">
          <div className="theme-item">
            <img src={starIcon} alt="Theme" className="setting-icon" />
            <p>Theme</p>
            <select value={theme} onChange={(e) => setTheme(e.target.value)}>
              {Object.keys(words).map((t) => (
                <option key={t} value={t}>{t}</option>
              ))}
            </select>
          </div>
          <hr class="underline"/>
          <p className="word-selection-instruction">Select one of the four words to draw</p>
          <div className="button-group">
            {words[theme].map((word) => (
              <button key={word} className="btn">{word}</button>))}
          </div>
        </div>
      </div>
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
    </div>
  );
};
export default GameSetup;
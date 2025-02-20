import React, {useState} from "react";
import "../styles/GameSetup.css";
import userIcon from "./assets//setupPage/user.png"
import clockIcon from "./assets//setupPage/clock.png"
import roundsIcon from "./assets//setupPage/rounds.png"
import gameModeIcon from "./assets//setupPage/gamemode.png"
import avatar from "./assets/avatar.png"
import inviteIcon from "./assets/setupPage/invite.png"
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
  const [players, setPlayers] = useState(5);
  const [drawTime, setDrawTime] = useState(60);
  const [rounds, setRounds] = useState(3);
  const [gameMode, setGameMode] = useState("Low");
  return (
    <div className="container">
      <h1 className="logo">InkBlink</h1>
      <hr class="underline"/>
      <h2 className="setup-heading">Setting Up...</h2>
      <div className="content-box">
        <div className="user-card">
          <img src={avatar} alt="User-Avatar" className="user-avatar"/>
          <div>
            <p className="user-name">Jerry</p>
            <p className="user-points">0 points</p>
          </div>
        </div>
        <div className="setup-box">
          <div className="setting-item">
            <img src={userIcon} alt="Players" className="setting-icon" />
            <p>Players</p>
            <select value="players" onChange={(e) => setPlayers(e.target.value)}>
              {[2,3,4,5,6].map((num) => (
                <option key={num} value={num}>{num}</option>
              ))}
            </select>
          </div>
          <div className="setting-item">
            <img src={clockIcon} alt="Drawtime" className="setting-icon" />
            <p>Drawtime</p>
            <select value="{drawTime}" onChange={(e) => setDrawTime(e.target.value)}>
              {[30, 45, 60, 90].map((num) => (
                <option key={num} value={num}>{num}</option>
              ))}
            </select>
          </div>
          <div className="setting-item">
            <img src={roundsIcon} alt="Rounds" className="setting icon" />
            <p>Rounds</p>
            <select value="{rounds}" onChange={(e) => setRounds(e.target.value)}>
              {[1, 2, 3, 4, 5].map((num) => (
                <option key={num} value={num}>{num}</option>
              ))}
            </select>
          </div>
          <div className="setting-item">
            <img src={gameModeIcon} alt="Game Mode" className="setting icon" />
            <p>Game Mode</p>
            <select value="{gameMode}" onChange={(e) => setGameMode(e.target.value)}>
              {["Easy", "Medium", "Hard"].map((num) => (
                <option key={num} value={num}>{num}</option>
              ))}
            </select>
          </div>
          <div className="button-group">
            <button className="btn">Let's Play!</button>
            <button className="invite-btn">
              <img src={inviteIcon} alt="Invite" />
              Invite
              </button>
          </div>
        </div>
      </div>
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
    </div>
  );
};
export default GameSetup;

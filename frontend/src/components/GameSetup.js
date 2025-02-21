import React, {useState} from "react";
import "../styles/GameSetup.css";
import { useNavigate } from "react-router-dom";
import userIcon from "./assets/setupPage/user.png"
import crayons from "./assets/setupPage/crayons.png"
import hourglass from "./assets/setupPage/hourglass.png"

import avatar from "./assets/avatar.png"
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";

const GameSetup = () => {
  const navigate = useNavigate();
  const [players, setPlayers]= useState([
    { name: "Mary", avatar: "🐌", score: 245 },
    { name: "Bob", avatar: "🌵", score: 7653 },
    { name: "Jeff", avatar: "😃", score: 176 },
    { name: "Sarah", avatar: "☀️", score: 45729 },
    { name: "James", avatar: "🦃", score: 2148 },
    { name: "Daniel", avatar: "💬", score: 40652 },
    { name: "Joe", avatar: "📚", score: 4155 },
    { name: "Olivia", avatar: "🥶", score: 42385 },
  ]);
  return (
    <div className="setup_container">
      <img src={left_bkg} alt="Left Top" className="left-top-image" />
      <img src={right_bkg} alt="Right Top" className="right-top-image" />
      <div className="setup_logo"><h1>Game Lobby</h1></div>
      <hr className="underline"/>
      <div className="setup_content_container">
        <div className="setup_join_box">
          <div className="setup_join_title">JOIN CODE:</div>
          <div className="setup_join_code">9K4K32</div>
        </div>
        <div className="setup_image">
          <img src={hourglass} alt="hourglass" className="setup_image hourglass" />
          <img src={crayons} alt="crayons" className="setup_image crayons" />
        </div>
        <div className="setup_players_box">
          <div className="setup_player_title">
            <img src={userIcon} alt="Players" className="setup-players" />
            <div className="setup_join_title">Players</div>
          </div>
          <div className="setup_player_list">
            {players.map((player, index) => (
              <div key={index} className="setup_player_card">
                  <span className="avatar">{player.avatar}</span>
                  <span className="name">{player.name}</span>
              </div>
            ))}
          </div>
          <div className="setup_btn_group">
            <button className="setup_btn" onClick={() => navigate("/game")}>Let's Play!</button>
          </div>
        </div>
      </div>      
    </div>
  );
};
export default GameSetup;
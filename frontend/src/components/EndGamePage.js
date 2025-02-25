import React from "react";
import { useNavigate } from "react-router-dom";
import "../styles/EndGamePage.css";
import right_bkg from "./assets/right_bkg.png";
import left_bkg from "./assets/left_bkg.png";
import "../styles/WelcomePage.css";

const EndGamePage = () => {
    const navigate = useNavigate();

    // temporary
    const players = [
        { username: "Mary", icon: "🐌", score: 245 },
        { username: "Bob", icon: "🌵", score: 7653 },
        { username: "Jeff", icon: "😃", score: 176 },
        { username: "Sarah", icon: "☀️", score: 45729 },
        { username: "James", icon: "🦃", score: 2148 },
        { username: "Daniel", icon: "💬", score: 40652 },
        { username: "Joe", icon: "📚", score: 4155 },
        { username: "Olivia", icon: "🥶", score: 42385 },
      ];

    // Sort players by score (highest to lowest)
    players.sort((a, b) => b.score - a.score);

    return (
        <div className="endgame_container">           
            <div className="endgame_box">
                <h1 className="end_logo">InkBlink</h1> 
                <h1 className="endgame_title">Game Over!</h1>
                
                <div className="endgame_list">
                    {players.map((player, index) => (
                        <div key={index} className="endgame_player">
                            <span className="endgame_rank">#{index + 1}</span>
                            <span className="endgame_avatar">{player.icon}</span>
                            <span className="endgame_name">{player.username}</span>
                            <span className="endgame_score">{player.score} pts</span>
                        </div>
                    ))}
                </div>

                <button className="endgame_btn" onClick={() => navigate("/")}>
                    Back to Home
                </button>
            </div>
        </div>
    );
};

export default EndGamePage;